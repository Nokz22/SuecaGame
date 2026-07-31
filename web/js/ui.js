import { Game, POINTS_TO_WIN } from './game.js';
import { cardLabel } from './cards.js';

const $ = selector => document.querySelector(selector);
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));

const BOT_DELAY = 750;
const PLAY_PAUSE = 220;
const TRICK_PAUSE = 1350;

const params = new URLSearchParams(location.search);

let game = null;
let backsCount = [0, 0, 0, 0];

// Os eventos do motor entram numa fila e são apresentados um a um,
// com as pausas certas — o motor decide as regras, a fila decide o ritmo.
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
    case 'round-start': return onRoundStart(event);
    case 'turn': return onTurn(event);
    case 'played': return onPlayed(event);
    case 'trick': return onTrick(event);
    case 'round-end': return onRoundEnd(event);
    default: return undefined;
  }
}

// ——— arranque ———

$('#start').addEventListener('click', startMatch);
$('#name').addEventListener('keydown', e => {
  if (e.key === 'Enter') {
    startMatch();
  }
});

function startMatch() {
  const name = $('#name').value.trim() || 'Jogador';
  game = new Game(name);
  if (params.has('demo')) {
    game.players[0].bot = true; // modo espetador: os 4 lugares jogam sozinhos
  }
  game.on(enqueue);
  $('#menu').hidden = true;
  $('#table').hidden = false;
  updateGamePoints([0, 0]);
  updateRoundPoints([0, 0]);
  game.startRound();
}

// ——— eventos ———

function onRoundStart(event) {
  for (let seat = 0; seat < 4; seat++) {
    const plate = $(`#plate-${seat}`);
    plate.innerHTML = '';
    plate.append(playerName(seat) + (seat === 0 ? ' (tu)' : ''));
    if (seat === event.dealer) {
      const tag = document.createElement('span');
      tag.className = 'dealer-tag';
      tag.textContent = ' · dá as cartas';
      plate.append(tag);
    }
    if (seat !== 0) {
      backsCount[seat] = 10;
      renderBacks(seat);
    }
    $(`#slot-${seat}`).innerHTML = '';
  }
  $('#banner').hidden = true;

  const trumpEl = $('#trumpcard');
  trumpEl.textContent = `${event.trump.value.short}${event.trump.suit.symbol}`;
  trumpEl.classList.toggle('red', event.trump.suit.red);
  $('#trumpbox').hidden = false;

  updateRoundPoints([0, 0]);
  renderHand(null);
}

async function onTurn(event) {
  for (let seat = 0; seat < 4; seat++) {
    $(`#plate-${seat}`).classList.toggle('active', seat === event.seat);
  }
  if (game.players[event.seat].bot) {
    renderHand(null);
    await sleep(BOT_DELAY);
    game.botPlay();
  } else {
    renderHand(new Set(game.legalCards(0).map(c => c.id)));
  }
}

async function onPlayed(event) {
  const slot = $(`#slot-${event.seat}`);
  slot.innerHTML = '';
  slot.append(cardElement(event.card));
  if (event.seat === 0) {
    renderHand(null);
  } else {
    backsCount[event.seat]--;
    renderBacks(event.seat);
  }
  await sleep(PLAY_PAUSE);
}

async function onTrick(event) {
  updateRoundPoints(event.roundPoints);
  const banner = $('#banner');
  banner.textContent = event.winnerSeat % 2 === 0
    ? `Vaza nossa! ${playerName(event.winnerSeat)} leva +${event.points}`
    : `Vaza de ${playerName(event.winnerSeat)} · +${event.points}`;
  banner.hidden = false;
  await sleep(TRICK_PAUSE);
  banner.hidden = true;
  for (let seat = 0; seat < 4; seat++) {
    $(`#slot-${seat}`).innerHTML = '';
  }
}

async function onRoundEnd(event) {
  updateRoundPoints(event.roundPoints);
  updateGamePoints(event.gamePoints);

  const title = $('#round-title');
  const detail = $('#round-detail');
  const button = $('#round-continue');

  if (event.over) {
    title.textContent = event.winnerTeam === 0 ? 'Vitória! 🎉' : 'Desta vez foi deles…';
    detail.innerHTML =
      `<div class="big">${event.gamePoints[0]} — ${event.gamePoints[1]}</div>` +
      `<div>${event.winnerTeam === 0 ? 'Tu e a Rosa ganharam a partida.' : 'A Beatriz e o Manel levaram a partida.'}</div>`;
    button.textContent = 'Jogar outra vez';
  } else {
    title.textContent = 'Fim da ronda';
    const gained = [gainedText(event.roundPoints[0]), gainedText(event.roundPoints[1])];
    detail.innerHTML =
      `<div class="big">Nós ${event.roundPoints[0]} — ${event.roundPoints[1]} Eles</div>` +
      `<div>${gained[0]} · ${gained[1]}</div>` +
      `<div>Jogos: ${event.gamePoints[0]} — ${event.gamePoints[1]} (à melhor de ${POINTS_TO_WIN})</div>`;
    button.textContent = 'Próxima ronda';
  }

  $('#round-overlay').hidden = false;
  await new Promise(resolve => button.addEventListener('click', resolve, { once: true }));
  $('#round-overlay').hidden = true;

  if (event.over) {
    startMatch();
  } else {
    game.nextRound();
  }
}

function gainedText(points) {
  if (points === 120) return 'capote: +4 jogos!';
  if (points >= 91) return `+2 jogos (${points} pts)`;
  if (points >= 61) return `+1 jogo (${points} pts)`;
  return `0 jogos (${points} pts)`;
}

// ——— render ———

function playerName(seat) {
  return game.players[seat].name;
}

function cardElement(card) {
  const el = document.createElement('button');
  el.type = 'button';
  el.className = 'card ' + (card.suit.red ? 'red' : 'black');
  el.dataset.id = card.id;
  el.setAttribute('aria-label', cardLabel(card));
  el.innerHTML =
    `<span class="corner tl">${card.value.short}<i>${card.suit.symbol}</i></span>` +
    `<span class="pip">${card.suit.symbol}</span>` +
    `<span class="corner br">${card.value.short}<i>${card.suit.symbol}</i></span>`;
  return el;
}

function renderHand(legalIds) {
  const hand = $('#hand');
  hand.innerHTML = '';
  for (const card of game.hands[0]) {
    const el = cardElement(card);
    el.disabled = !legalIds || !legalIds.has(card.id);
    el.addEventListener('click', () => {
      if (!el.disabled) {
        game.playCard(0, card);
      }
    });
    hand.append(el);
  }
}

function renderBacks(seat) {
  const box = $(`#backs-${seat}`);
  box.innerHTML = '';
  for (let i = 0; i < backsCount[seat]; i++) {
    const back = document.createElement('div');
    back.className = 'card back';
    box.append(back);
  }
}

function updateRoundPoints(points) {
  $('#pts-0').textContent = points[0];
  $('#pts-1').textContent = points[1];
}

function updateGamePoints(gamePoints) {
  for (const team of [0, 1]) {
    const dots = $(`#dots-${team}`);
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

// atalho partilhável: sueca.../?jogar=Nome entra logo na mesa
if (params.has('jogar')) {
  $('#name').value = params.get('jogar');
  startMatch();
}
