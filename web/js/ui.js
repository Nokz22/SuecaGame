import { LocalSession, HostSession, GuestSession } from './sessions.js';
import { createRoom, joinRoom, normalizeCode } from './net.js';
import { cardElement } from './cardface.js';
import { isValidPlay } from './rules.js';
import { POINTS_TO_WIN } from './game.js';

const $ = selector => document.querySelector(selector);
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));

const PLAY_PAUSE = 220;
const TRICK_PAUSE = 1350;

const params = new URLSearchParams(location.search);

// ——— estado da vista (alimentado só por eventos da sessão) ———

let session = null;
let generation = 0;      // invalida callbacks de sessões antigas
let roomCode = null;
let myHand = [];
let trickCards = [];     // cartas da vaza corrente, pela ordem de jogada
let currentSeat = -1;
let dealerSeat = -1;
let backsCount = [0, 0, 0, 0]; // por lugar (seat)

// Os eventos entram numa fila e são apresentados um a um com as pausas
// certas — a sessão decide as regras, a fila decide o ritmo.
const queue = [];
let pumping = false;

function enqueue(event) {
  queue.push(event);
  if (!pumping) {
    pump();
  }
}

async function pump() {
  pumping = true;
  while (queue.length) {
    await handle(queue.shift());
  }
  pumping = false;
}

async function handle(event) {
  switch (event.type) {
    case 'roster': return onRoster(event);
    case 'round-start': return onRoundStart(event);
    case 'hand': return onHand(event);
    case 'turn': return onTurn(event);
    case 'played': return onPlayed(event);
    case 'trick': return onTrick(event);
    case 'round-end': return onRoundEnd(event);
    case 'invalid': return undefined; // a UI já bloqueia cartas ilegais
    case 'host-left': return onHostLeft();
    default: return undefined;
  }
}

// ——— lugares: cada jogador vê-se a si próprio em baixo ———

function posOfSeat(seat) {
  return (seat - session.mySeat + 4) % 4; // 0=baixo(eu) 1=esq 2=cima 3=dir
}

function seatAtPos(pos) {
  return (session.mySeat + pos) % 4;
}

function usTeam() {
  return session.mySeat % 2;
}

function nameOf(seat) {
  const name = session.names[seat] ?? `Lugar ${seat}`;
  return seat === session.mySeat ? `${name} (tu)` : name;
}

// ——— menu ———

$('#start-single').addEventListener('click', () => startSingle(false));
$('#start-host').addEventListener('click', startHost);
$('#start-join').addEventListener('click', startJoin);
$('#name').addEventListener('keydown', e => {
  if (e.key === 'Enter') {
    startSingle(false);
  }
});
$('#join-code').addEventListener('keydown', e => {
  if (e.key === 'Enter') {
    startJoin();
  }
});

function playerName() {
  return $('#name').value.trim() || 'Jogador';
}

function menuError(message) {
  $('#menu-error').textContent = message;
  menuBusy(false);
}

function menuBusy(busy, message = '') {
  for (const id of ['#start-single', '#start-host', '#start-join']) {
    $(id).disabled = busy;
  }
  $('#menu-error').textContent = message;
}

function startSingle(demo) {
  closeSession();
  session = new LocalSession(playerName(), { demo });
  roomCode = null;
  wireSession();
  showTable();
  session.start();
}

async function startHost() {
  closeSession();
  menuBusy(true, 'A criar a sala…');
  try {
    const { code, transport } = await createRoom();
    session = new HostSession(playerName(), transport);
    roomCode = code;
    wireSession();
    menuBusy(false);
    showLobby();
  } catch (err) {
    menuError(err.message);
  }
}

async function startJoin() {
  const code = normalizeCode($('#join-code').value);
  if (!code) {
    menuError('Escreve o código da sala.');
    return;
  }
  closeSession();
  menuBusy(true, 'A ligar à sala…');
  try {
    const connection = await joinRoom(code);
    session = await GuestSession.join(playerName(), connection);
    roomCode = code;
    wireSession();
    menuBusy(false);
    showLobby();
  } catch (err) {
    menuError(err.message);
  }
}

function wireSession() {
  const gen = ++generation;
  session.onEvent(event => {
    if (gen === generation) {
      enqueue(event);
    }
  });
}

function closeSession() {
  generation++;
  queue.length = 0;
  pumping = false;
  if (session) {
    session.close();
    session = null;
  }
}

// ——— navegação entre ecrãs ———

function showMenu() {
  closeSession();
  $('#round-overlay').hidden = true;
  $('#lobby').hidden = true;
  $('#table').hidden = true;
  $('#menu').hidden = false;
  menuBusy(false);
}

function showLobby() {
  $('#menu').hidden = true;
  $('#table').hidden = true;
  $('#room-code').textContent = roomCode;
  $('#lobby-start').hidden = !session.isHost;
  $('#lobby-info').textContent = session.isHost
    ? 'Os lugares vazios serão preenchidos por bots.'
    : 'À espera que o anfitrião comece o jogo…';
  renderLobbySeats();
  $('#lobby').hidden = false;
}

function showTable() {
  $('#menu').hidden = true;
  $('#lobby').hidden = true;
  $('#round-overlay').hidden = true;
  updateGamePoints([0, 0]);
  updateRoundPoints([0, 0]);
  $('#table').hidden = false;
}

$('#lobby-leave').addEventListener('click', showMenu);
$('#lobby-start').addEventListener('click', () => session && session.start());
$('#room-code').addEventListener('click', async () => {
  const link = `${location.origin}${location.pathname}?sala=${roomCode}`;
  try {
    await navigator.clipboard.writeText(link);
    $('#room-hint').textContent = 'Convite copiado! Envia o link a quem quiseres.';
  } catch {
    $('#room-hint').textContent = `Link de convite: ${link}`;
  }
});

function renderLobbySeats() {
  const box = $('#lobby-seats');
  box.innerHTML = '';
  for (let seat = 0; seat < 4; seat++) {
    const row = document.createElement('div');
    row.className = 'lobby-seat';
    const team = seat % 2 === session.mySeat % 2 ? 'a tua equipa' : 'equipa adversária';
    const name = session.names[seat];
    row.textContent = name !== undefined
      ? `${nameOf(seat)} — ${team}`
      : `— lugar livre (${team}) —`;
    box.append(row);
  }
}

// ——— eventos de jogo ———

function onRoster(event) {
  if (!$('#lobby').hidden) {
    renderLobbySeats();
  }
  if (!$('#table').hidden) {
    for (let pos = 0; pos < 4; pos++) {
      renderPlate(seatAtPos(pos));
    }
  }
}

function onRoundStart(event) {
  if ($('#table').hidden) {
    showTable(); // convidados saltam do lobby para a mesa
  }
  dealerSeat = event.dealer;
  trickCards = [];
  myHand = [];
  for (let pos = 0; pos < 4; pos++) {
    const seat = seatAtPos(pos);
    renderPlate(seat);
    $(`#slot-${pos}`).innerHTML = '';
    if (seat !== session.mySeat) {
      backsCount[seat] = 10;
      renderBacks(seat);
    }
  }
  $('#banner').hidden = true;
  $('#round-overlay').hidden = true;

  const trumpEl = $('#trumpcard');
  trumpEl.innerHTML = '';
  const mini = cardElement(event.trump);
  mini.classList.add('mini');
  mini.disabled = true;
  trumpEl.append(mini);
  $('#trumpbox').hidden = false;

  updateRoundPoints([0, 0]);
  renderHand(false);
}

function onHand(event) {
  myHand = [...event.cards];
  renderHand(currentSeat === session.mySeat, { deal: true });
}

function onTurn(event) {
  currentSeat = event.seat;
  for (let pos = 0; pos < 4; pos++) {
    const seat = seatAtPos(pos);
    $(`#plate-${pos}`).classList.toggle('active', seat === currentSeat);
  }
  renderHand(currentSeat === session.mySeat);
}

async function onPlayed(event) {
  const pos = posOfSeat(event.seat);
  const slot = $(`#slot-${pos}`);
  slot.innerHTML = '';
  slot.append(cardElement(event.card));
  trickCards.push(event.card);
  if (event.seat === session.mySeat) {
    myHand = myHand.filter(c => c.id !== event.card.id);
    renderHand(false);
  } else {
    backsCount[event.seat]--;
    renderBacks(event.seat);
  }
  await sleep(PLAY_PAUSE);
}

async function onTrick(event) {
  updateRoundPoints(event.roundPoints);
  const banner = $('#banner');
  const ours = event.winnerSeat % 2 === usTeam();
  const winner = session.names[event.winnerSeat] ?? '?';
  banner.textContent = ours
    ? `Vaza nossa! ${winner} leva +${event.points}`
    : `Vaza de ${winner} · +${event.points}`;
  banner.hidden = false;
  await sleep(TRICK_PAUSE);
  banner.hidden = true;
  // as cartas deslizam para o vencedor antes de sair da mesa
  const pile = $('#pile');
  pile.classList.add(`collect-p${posOfSeat(event.winnerSeat)}`);
  await sleep(430);
  pile.className = '';
  trickCards = [];
  for (let pos = 0; pos < 4; pos++) {
    $(`#slot-${pos}`).innerHTML = '';
  }
}

async function onRoundEnd(event) {
  updateRoundPoints(event.roundPoints);
  updateGamePoints(event.gamePoints);

  const us = usTeam();
  const title = $('#round-title');
  const detail = $('#round-detail');
  const button = $('#round-continue');

  if (event.over) {
    const won = event.winnerTeam === us;
    title.textContent = won ? 'Vitória! 🎉' : 'Desta vez foi deles…';
    detail.innerHTML =
      `<div class="big">${event.gamePoints[us]} — ${event.gamePoints[1 - us]}</div>` +
      `<div>${won ? 'A tua equipa ganhou a partida.' : 'A equipa adversária levou a partida.'}</div>`;
    button.textContent = session instanceof LocalSession ? 'Jogar outra vez' : 'Voltar ao menu';
    button.hidden = false;
    $('#round-overlay').hidden = false;
    await waitClick(button);
    if (session instanceof LocalSession) {
      $('#round-overlay').hidden = true;
      startSingle(false);
    } else {
      showMenu();
    }
    return;
  }

  title.textContent = 'Fim da ronda';
  detail.innerHTML =
    `<div class="big">Nós ${event.roundPoints[us]} — ${event.roundPoints[1 - us]} Eles</div>` +
    `<div>${gainedText(event.roundPoints[us])} · ${gainedText(event.roundPoints[1 - us])}</div>` +
    `<div>Jogos: ${event.gamePoints[us]} — ${event.gamePoints[1 - us]} (à melhor de ${POINTS_TO_WIN})</div>`;

  if (session.isHost) {
    button.textContent = 'Próxima ronda';
    button.hidden = false;
    $('#round-overlay').hidden = false;
    await waitClick(button);
    $('#round-overlay').hidden = true;
    session.nextRound();
  } else {
    button.hidden = true;
    $('#round-overlay').hidden = false; // o round-start do anfitrião fecha-o
  }
}

function onHostLeft() {
  if (!session || session.isHost) {
    return;
  }
  const title = $('#round-title');
  const detail = $('#round-detail');
  const button = $('#round-continue');
  title.textContent = 'O anfitrião saiu';
  detail.innerHTML = '<div>A ligação à sala terminou.</div>';
  button.textContent = 'Voltar ao menu';
  button.hidden = false;
  $('#round-overlay').hidden = false;
  waitClick(button).then(showMenu);
}

function gainedText(points) {
  if (points === 120) return 'capote: +4 jogos!';
  if (points >= 91) return `+2 jogos (${points} pts)`;
  if (points >= 61) return `+1 jogo (${points} pts)`;
  return `0 jogos (${points} pts)`;
}

function waitClick(button) {
  return new Promise(resolve => button.addEventListener('click', resolve, { once: true }));
}

// ——— render ———

function renderPlate(seat) {
  const plate = $(`#plate-${posOfSeat(seat)}`);
  plate.innerHTML = '';
  const name = session.names[seat] ?? `Lugar ${seat}`;
  const medal = document.createElement('span');
  medal.className = 'medal ' + (seat % 2 === usTeam() ? 'us' : 'them');
  medal.textContent = name.charAt(0).toUpperCase();
  const label = document.createElement('span');
  label.className = 'pname';
  label.textContent = nameOf(seat);
  plate.append(medal, label);
  if (seat === dealerSeat) {
    const tag = document.createElement('span');
    tag.className = 'dealer-tag';
    tag.textContent = '· dá as cartas';
    plate.append(tag);
  }
}

function renderHand(myTurn, { deal = false } = {}) {
  const hand = $('#hand');
  hand.innerHTML = '';
  const leadCard = trickCards.length ? trickCards[0] : null;
  const n = myHand.length;
  myHand.forEach((card, i) => {
    const el = cardElement(card);
    // leque: cada carta conhece a sua posição para rodar/subir em CSS
    el.style.setProperty('--i', i);
    el.style.setProperty('--n', n);
    if (deal) {
      el.classList.add('deal');
    }
    const legal = isValidPlay(card, myHand, leadCard);
    el.disabled = !myTurn || !legal;
    el.addEventListener('click', () => {
      if (!el.disabled && session) {
        renderHand(false); // evita duplo clique enquanto a jogada viaja
        session.playCard(card);
      }
    });
    hand.append(el);
  });
}

function renderBacks(seat) {
  const box = $(`#backs-${posOfSeat(seat)}`);
  if (!box) {
    return;
  }
  box.innerHTML = '';
  for (let i = 0; i < backsCount[seat]; i++) {
    const back = document.createElement('div');
    back.className = 'card back';
    box.append(back);
  }
}

function updateRoundPoints(points) {
  const us = usTeam();
  $('#pts-us').textContent = points[us];
  $('#pts-them').textContent = points[1 - us];
}

function updateGamePoints(gamePoints) {
  const us = usTeam();
  for (const [id, team] of [['#dots-us', us], ['#dots-them', 1 - us]]) {
    const dots = $(id);
    dots.innerHTML = '';
    for (let i = 0; i < POINTS_TO_WIN; i++) {
      const dot = document.createElement('span');
      dot.textContent = '◆';
      if (i >= gamePoints[team]) {
        dot.className = 'off';
      }
      dots.append(dot);
    }
  }
}

// ——— atalhos por URL ———

if (params.has('sala')) {
  $('#join-code').value = normalizeCode(params.get('sala'));
  $('#name').focus();
} else if (params.has('jogar')) {
  $('#name').value = params.get('jogar');
  startSingle(params.has('demo'));
} else if (params.has('demo')) {
  startSingle(true);
}
