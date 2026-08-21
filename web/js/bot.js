import { isValidPlay, trickWinner } from './rules.js?v=__V__';

// Heurísticas de Sueca (as mesmas da versão desktop):
// - a abrir, sai com a carta mais fraca sem gastar trunfos
// - se o parceiro vai a ganhar, carrega a vaza com pontos sem o cortar
// - se o adversário vai a ganhar, ganha com a carta mais barata
//   (ou a mais valiosa, se for o último a jogar)
// - se não pode ganhar, desfaz-se da carta com menos pontos

export function chooseBotCard(hand, trick, trumpKey, seat) {
  const leadCard = trick.length ? trick[0].card : null;
  const legal = hand.filter(c => isValidPlay(c, hand, leadCard));

  if (!trick.length) {
    return lead(legal, trumpKey);
  }

  const played = trick.map(p => p.card);
  const winnerSeat = trick[trickWinner(played, trumpKey)].seat;
  const partnerWinning = winnerSeat === (seat + 2) % 4;
  const winning = legal.filter(c => wouldWin(c, played, trumpKey));

  if (partnerWinning) {
    const safe = legal.filter(c => !winning.includes(c));
    if (safe.length) {
      return pick(safe, (a, b) =>
        b.value.points - a.value.points || a.value.strength - b.value.strength);
    }
    return pick(winning, byWeakness(leadCard, trumpKey));
  }

  if (winning.length) {
    const lastToPlay = trick.length === 3;
    if (lastToPlay) {
      return pick(winning, (a, b) =>
        b.value.points - a.value.points ||
        strengthIn(a, leadCard, trumpKey) - strengthIn(b, leadCard, trumpKey));
    }
    return pick(winning, byWeakness(leadCard, trumpKey));
  }

  return pick(legal, (a, b) =>
    a.value.points - b.value.points || a.value.strength - b.value.strength);
}

function lead(legal, trumpKey) {
  const nonTrump = legal.filter(c => c.suit.key !== trumpKey);
  const pool = nonTrump.length ? nonTrump : legal;
  return pick(pool, (a, b) =>
    a.value.points - b.value.points || a.value.strength - b.value.strength);
}

function wouldWin(candidate, played, trumpKey) {
  const cards = [...played, candidate];
  return trickWinner(cards, trumpKey) === cards.length - 1;
}

// trunfos acima do naipe de saída, resto abaixo
function strengthIn(card, leadCard, trumpKey) {
  if (card.suit.key === trumpKey) return 200 + card.value.strength;
  if (leadCard && card.suit.key === leadCard.suit.key) return 100 + card.value.strength;
  return card.value.strength;
}

function byWeakness(leadCard, trumpKey) {
  return (a, b) => strengthIn(a, leadCard, trumpKey) - strengthIn(b, leadCard, trumpKey);
}

function pick(cards, compare) {
  return [...cards].sort(compare)[0];
}
