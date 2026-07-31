export function isValidPlay(card, hand, leadCard) {
  if (!leadCard) {
    return true;
  }
  const hasSuit = hand.some(c => c.suit.key === leadCard.suit.key);
  return hasSuit ? card.suit.key === leadCard.suit.key : true;
}

function beats(challenger, current, trumpKey) {
  if (challenger.suit.key === trumpKey && current.suit.key !== trumpKey) return true;
  if (current.suit.key === trumpKey && challenger.suit.key !== trumpKey) return false;
  if (challenger.suit.key === current.suit.key) {
    return challenger.value.strength > current.value.strength;
  }
  return false;
}

export function trickWinner(cards, trumpKey) {
  let winner = 0;
  for (let i = 1; i < cards.length; i++) {
    if (beats(cards[i], cards[winner], trumpKey)) {
      winner = i;
    }
  }
  return winner;
}

export function trickPoints(cards) {
  return cards.reduce((sum, c) => sum + c.value.points, 0);
}

export function gamePointsFor(roundPoints) {
  if (roundPoints === 120) return 4;   // capote
  if (roundPoints >= 91) return 2;
  if (roundPoints >= 61) return 1;
  return 0;
}
