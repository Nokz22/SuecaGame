// Auto-teste do motor: joga centenas de partidas completas com bots
// e verifica as invariantes da Sueca em todas as rondas.
import { Game } from '../js/game.js';
import { gamePointsFor, isValidPlay } from '../js/rules.js';

let assertions = 0;

function assert(condition, message) {
  assertions++;
  if (!condition) {
    console.error('FALHOU: ' + message);
    process.exit(1);
  }
}

// pontuação nas fronteiras
assert(gamePointsFor(60) === 0, '60 pontos não vale jogos');
assert(gamePointsFor(61) === 1, '61 pontos vale 1 jogo');
assert(gamePointsFor(90) === 1, '90 pontos vale 1 jogo');
assert(gamePointsFor(91) === 2, '91 pontos vale 2 jogos');
assert(gamePointsFor(119) === 2, '119 pontos vale 2 jogos');
assert(gamePointsFor(120) === 4, 'capote vale 4 jogos');

const MATCHES = 300;

for (let m = 0; m < MATCHES; m++) {
  const game = new Game('Bot Zero');
  game.players[0].bot = true; // todos bots

  let tricksInRound = 0;
  let pointsInRound = 0;
  let rounds = 0;
  let finished = false;

  game.on(event => {
    if (event.type === 'played') {
      // a jogada emitida tinha de ser legal no momento em que foi feita
      assert(event.card && event.card.id, 'carta jogada tem identidade');
    }
    if (event.type === 'trick') {
      tricksInRound++;
      pointsInRound += event.points;
    }
    if (event.type === 'round-end') {
      rounds++;
      assert(tricksInRound === 10, `ronda com ${tricksInRound} vazas (esperava 10)`);
      assert(pointsInRound === 120, `ronda com ${pointsInRound} pontos (esperava 120)`);
      assert(event.roundPoints[0] + event.roundPoints[1] === 120,
        'os pontos das equipas têm de somar 120');
      tricksInRound = 0;
      pointsInRound = 0;
      if (event.over) {
        assert(event.gamePoints[event.winnerTeam] >= 4, 'vencedor tem 4+ jogos');
        finished = true;
      }
    }
  });

  game.startRound();
  let safety = 0;
  while (!finished && safety++ < 20000) {
    if (game.isBotTurn()) {
      assert(game.botPlay(), 'o bot tem de conseguir jogar sempre');
    } else if (game.phase === 'between-rounds') {
      game.nextRound();
    } else {
      break;
    }
  }
  assert(finished, `partida ${m} não terminou (fase: ${game.phase}, rondas: ${rounds})`);
}

// validação de assistência: com o naipe na mão, só o naipe é legal
import { SUITS, VALUES } from '../js/cards.js';
const copasBaixa = { suit: SUITS.COPAS, value: VALUES[9], id: 'COPAS:DOIS' };
const ourosAlta = { suit: SUITS.OUROS, value: VALUES[0], id: 'OUROS:AS' };
const lead = { suit: SUITS.COPAS, value: VALUES[4], id: 'COPAS:DAMA' };
assert(isValidPlay(copasBaixa, [copasBaixa, ourosAlta], lead), 'assistir é legal');
assert(!isValidPlay(ourosAlta, [copasBaixa, ourosAlta], lead), 'renunciar é ilegal');

console.log(`OK — ${MATCHES} partidas completas, ${assertions} verificações.`);
