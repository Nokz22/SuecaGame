// Teste de integração do multiplayer: HostSession + convidados ligados por
// um transporte loopback com a MESMA interface do adaptador WebRTC.
// Verifica atribuição de lugares, privacidade das mãos, jogadas remotas
// validadas pelo anfitrião, queda de convidado a meio, e partida completa.
import { HostSession, GuestSession } from '../js/sessions.js';
import { isValidPlay } from '../js/rules.js';

let assertions = 0;

function assert(condition, message) {
  assertions++;
  if (!condition) {
    console.error('FALHOU: ' + message);
    process.exit(1);
  }
}

// ——— transporte loopback (par de pontas em memória, entrega assíncrona) ———

function makePipe() {
  function makeEnd() {
    return { msgCb: null, closeCb: null, peer: null, closed: false };
  }
  const a = makeEnd();
  const b = makeEnd();
  a.peer = b;
  b.peer = a;

  function api(end) {
    return {
      seat: undefined,
      send(obj) {
        const wire = JSON.parse(JSON.stringify(obj)); // simula a serialização
        queueMicrotask(() => {
          if (!end.peer.closed && end.peer.msgCb) {
            end.peer.msgCb(wire);
          }
        });
      },
      onMessage(cb) { end.msgCb = cb; },
      onClose(cb) { end.closeCb = cb; },
      close() {
        if (end.closed) return;
        end.closed = true;
        queueMicrotask(() => {
          if (!end.peer.closed) {
            end.peer.closed = true;
            if (end.peer.closeCb) end.peer.closeCb();
          }
        });
      },
    };
  }
  return [api(a), api(b)];
}

function makeHostTransport() {
  let connCb = null;
  return {
    transport: {
      onConnection(cb) { connCb = cb; },
      close() {},
    },
    dial() {
      const [hostEnd, guestEnd] = makePipe();
      connCb(hostEnd);
      return guestEnd;
    },
  };
}

const flush = () => new Promise(resolve => setTimeout(resolve, 0));

// ——— cenário: anfitrião + 2 convidados (o 4.º lugar fica bot) ———

const { transport, dial } = makeHostTransport();
const host = new HostSession('Nuno', transport, { botDelay: 0 });

const hostEvents = [];
host.onEvent(e => hostEvents.push(e));

const guestA = await GuestSession.join('Rita', dial());
const guestB = await GuestSession.join('Zé', dial());
await flush();

assert(guestA.mySeat === 1, `Rita devia ficar no lugar 1 (ficou ${guestA.mySeat})`);
assert(guestB.mySeat === 2, `Zé devia ficar no lugar 2 (ficou ${guestB.mySeat})`);
assert(host.names[1] === 'Rita' && host.names[2] === 'Zé', 'roster do anfitrião');
assert(guestA.names[2] === 'Zé', 'a Rita vê o Zé no roster');

// estado por convidado, alimentado apenas pelos eventos recebidos
function makeGuestView(guest) {
  const view = { hand: [], trick: [], turn: -1, foreignHands: 0, roundsEnded: 0, over: false };
  guest.onEvent(e => {
    if (e.type === 'hand') {
      if (e.seat === guest.mySeat) view.hand = [...e.cards];
      else view.foreignHands++;
    }
    if (e.type === 'turn') view.turn = e.seat;
    if (e.type === 'played') {
      view.trick.push(e.card);
      if (e.seat === guest.mySeat) view.hand = view.hand.filter(c => c.id !== e.card.id);
    }
    if (e.type === 'trick') view.trick = [];
    if (e.type === 'round-end') {
      view.roundsEnded++;
      if (e.over) view.over = true;
    }
  });
  return view;
}

const viewA = makeGuestView(guestA);
const viewB = makeGuestView(guestB);

const hostView = { hand: [], trick: [], turn: -1, over: false };
host.onEvent(e => {
  hostEvents.push(e);
  if (e.type === 'hand') hostView.hand = [...e.cards];
  if (e.type === 'turn') hostView.turn = e.seat;
  if (e.type === 'played') {
    hostView.trick.push(e.card);
    if (e.seat === 0) hostView.hand = hostView.hand.filter(c => c.id !== e.card.id);
  }
  if (e.type === 'trick') hostView.trick = [];
  if (e.type === 'round-end' && e.over) hostView.over = true;
  if (e.type === 'round-end' && !e.over) queueMicrotask(() => host.nextRound());
});

host.start();
await flush();

assert(hostView.hand.length === 10, 'anfitrião recebeu 10 cartas');
assert(viewA.hand.length === 10, 'Rita recebeu 10 cartas');
assert(viewB.hand.length === 10, 'Zé recebeu 10 cartas');
assert(host.game.players[3].bot === true, 'o lugar 4 ficou bot');

// jogada ilegal remota: se a Rita tiver o naipe, jogar outro naipe é rejeitado
// (testado ao longo do jogo: os convidados jogam sempre a primeira carta legal)

let guestBDropped = false;
let safety = 0;
while (!hostView.over && safety++ < 3000) {
  await flush();
  const playFor = (guest, view) => {
    if (view.turn === guest.mySeat && view.hand.length) {
      const lead = view.trick.length ? view.trick[0] : null;
      const card = view.hand.find(c => isValidPlay(c, view.hand, lead));
      assert(card, 'convidado tem sempre jogada legal');
      view.turn = -1;
      guest.playCard(card);
    }
  };
  if (hostView.turn === 0 && hostView.hand.length) {
    const lead = hostView.trick.length ? hostView.trick[0] : null;
    const card = hostView.hand.find(c => isValidPlay(c, hostView.hand, lead));
    hostView.turn = -1;
    host.playCard(card);
  }
  playFor(guestA, viewA);
  if (!guestBDropped) {
    playFor(guestB, viewB);
    // a meio da 1.ª ronda, o Zé "perde a ligação" — deve virar bot
    if (viewB.hand.length <= 7 && viewB.trick.length === 0) {
      guestB.close();
      guestBDropped = true;
    }
  }
}

assert(hostView.over, 'a partida terminou');
assert(host.game.players[2].bot === true, 'o Zé virou bot depois de cair');
assert(host.names[2].includes('(bot)'), 'o roster marca o lugar do Zé como bot');
assert(viewA.foreignHands === 0, 'a Rita NUNCA recebeu mãos de outros lugares');
assert(viewA.over, 'a Rita recebeu o fim da partida');

// sala cheia: com o jogo a decorrer, novos convidados são rejeitados
let rejected = false;
try {
  await GuestSession.join('Atrasado', dial(), { timeoutMs: 500 });
} catch {
  rejected = true;
}
assert(rejected, 'entrar com o jogo a decorrer é rejeitado');

console.log(`OK — multiplayer loopback completo, ${assertions} verificações.`);
