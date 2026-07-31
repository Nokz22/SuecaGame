export const SUITS = {
  OUROS:   { key: 'OUROS',   name: 'Ouros',   symbol: '♦', red: true },
  ESPADAS: { key: 'ESPADAS', name: 'Espadas', symbol: '♠', red: false },
  PAUS:    { key: 'PAUS',    name: 'Paus',    symbol: '♣', red: false },
  COPAS:   { key: 'COPAS',   name: 'Copas',   symbol: '♥', red: true },
};

export const VALUES = [
  { key: 'AS',     short: 'A', name: 'Ás',     strength: 10, points: 11 },
  { key: 'SETE',   short: '7', name: '7',      strength: 9,  points: 10 },
  { key: 'REI',    short: 'R', name: 'Rei',    strength: 8,  points: 4 },
  { key: 'VALETE', short: 'V', name: 'Valete', strength: 7,  points: 3 },
  { key: 'DAMA',   short: 'D', name: 'Dama',   strength: 6,  points: 2 },
  { key: 'SEIS',   short: '6', name: '6',      strength: 5,  points: 0 },
  { key: 'CINCO',  short: '5', name: '5',      strength: 4,  points: 0 },
  { key: 'QUATRO', short: '4', name: '4',      strength: 3,  points: 0 },
  { key: 'TRES',   short: '3', name: '3',      strength: 2,  points: 0 },
  { key: 'DOIS',   short: '2', name: '2',      strength: 1,  points: 0 },
];

export function makeDeck() {
  const deck = [];
  for (const suit of Object.values(SUITS)) {
    for (const value of VALUES) {
      deck.push({ suit, value, id: `${suit.key}:${value.key}` });
    }
  }
  return deck;
}

export function shuffle(cards, rng = Math.random) {
  for (let i = cards.length - 1; i > 0; i--) {
    const j = Math.floor(rng() * (i + 1));
    [cards[i], cards[j]] = [cards[j], cards[i]];
  }
  return cards;
}

// naipes alternam cor na mão para ser mais fácil de ler
const SUIT_ORDER = { PAUS: 0, COPAS: 1, ESPADAS: 2, OUROS: 3 };

export function sortHand(hand) {
  hand.sort((a, b) =>
    SUIT_ORDER[a.suit.key] - SUIT_ORDER[b.suit.key] ||
    b.value.strength - a.value.strength);
  return hand;
}

export function cardLabel(card) {
  return `${card.value.name} de ${card.suit.name}`;
}
