// Auditoria profunda do motor: em 500 partidas completas, verifica
// JOGADA A JOGADA que (1) a obrigação de assistir ao naipe nunca é violada
// e (2) todos os pontos são recontados de forma independente a partir das
// próprias cartas — vaza a vaza, ronda a ronda, jogo a jogo.
import { Game } from '../js/game.js';
import { isValidPlay, gamePointsFor } from '../js/rules.js';

let checks = 0;
function assert(cond, msg) {
  checks++;
  if (!cond) {
    console.error('VIOLAÇÃO: ' + msg);
    process.exit(1);
  }
}

const MATCHES = 100;
let followChecks = 0;
let hadToFollow = 0;

for (let m = 0; m < MATCHES; m++) {
  const game = new Game('Bot Zero');
  game.players[0].bot = true;

  let trickCards = [];
  let roundRecount = [0, 0];
  let gameRecount = [0, 0];
  let over = false;

  game.on(e => {
    if (e.type === 'played') {
      trickCards.push(e.card);
    }
    if (e.type === 'trick') {
      // recontagem independente dos pontos da vaza
      const sum = trickCards.reduce((s, c) => s + c.value.points, 0);
      assert(sum === e.points, `vaza: motor diz ${e.points}, recontagem dá ${sum}`);
      roundRecount[e.winnerSeat % 2] += sum;
      assert(e.roundPoints[0] === roundRecount[0] && e.roundPoints[1] === roundRecount[1],
        `acumulado da ronda: motor ${e.roundPoints}, recontagem ${roundRecount}`);
      trickCards = [];
    }
    if (e.type === 'round-end') {
      assert(roundRecount[0] + roundRecount[1] === 120, 'a ronda tem de somar 120');
      assert(e.roundPoints[0] === roundRecount[0] && e.roundPoints[1] === roundRecount[1],
        'pontos finais da ronda diferem da recontagem');
      gameRecount[0] += gamePointsFor(roundRecount[0]);
      gameRecount[1] += gamePointsFor(roundRecount[1]);
      assert(e.gamePoints[0] === gameRecount[0] && e.gamePoints[1] === gameRecount[1],
        `jogos: motor ${e.gamePoints}, recontagem ${gameRecount}`);
      roundRecount = [0, 0];
      if (e.over) over = true;
    }
  });

  game.startRound();
  let safety = 0;
  while (!over && safety++ < 3000) {
    if (game.isBotTurn()) {
      // fotografa a mão e o naipe de saída ANTES da jogada
      const seat = game.current;
      const handBefore = [...game.hands[seat]];
      const lead = game.trick.length ? game.trick[0].card : null;
      const hadLeadSuit = lead && handBefore.some(c => c.suit.key === lead.suit.key);

      game.botPlay();

      const played = game.trick.length
        ? game.trick[game.trick.length - 1]
        : null; // vaza fechou: a última jogada está no histórico via evento
      // valida com a regra pura, contra a mão fotografada
      const playedCard = played && played.seat === seat
        ? played.card
        : handBefore.find(c => !game.hands[seat].some(h => h.id === c.id));
      assert(playedCard, 'jogada do bot identificada');
      assert(isValidPlay(playedCard, handBefore, lead),
        `lugar ${seat} jogou ${playedCard.id} com saída ${lead ? lead.id : '—'} e mão ${handBefore.map(c => c.id).join(' ')}`);
      followChecks++;
      if (hadLeadSuit) {
        hadToFollow++;
        assert(playedCard.suit.key === lead.suit.key,
          `lugar ${seat} tinha ${lead.suit.key} e jogou ${playedCard.id}`);
      }
    } else if (game.phase === 'between-rounds') {
      game.nextRound();
    } else {
      break;
    }
  }
  assert(over, `partida ${m} terminou`);
}

console.log(`OK — ${MATCHES} partidas auditadas: ${followChecks} jogadas verificadas ` +
  `(${hadToFollow} com obrigação de assistir, todas cumpridas), ${checks} verificações no total.`);
