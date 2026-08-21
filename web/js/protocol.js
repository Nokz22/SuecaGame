import { cardFromId } from './cards.js?v=__V__';

// Eventos viajam como JSON entre anfitrião e convidados (WebRTC DataChannel).
// As cartas são achatadas para ids "NAIPE:VALOR" e reconstruídas à chegada,
// para o fio nunca transportar os objetos internos do motor.

export function encodeEvent(event) {
  switch (event.type) {
    case 'round-start':
      return { ...event, trump: event.trump.id };
    case 'played':
      return { ...event, card: event.card.id };
    case 'hand':
      return { ...event, cards: event.cards.map(c => c.id) };
    default:
      return { ...event };
  }
}

export function decodeEvent(event) {
  switch (event.type) {
    case 'round-start':
      return { ...event, trump: cardFromId(event.trump) };
    case 'played':
      return { ...event, card: cardFromId(event.card) };
    case 'hand':
      return { ...event, cards: event.cards.map(cardFromId) };
    default:
      return { ...event };
  }
}

/** Nomes sem espaço para partir layouts nem conteúdo malicioso. */
export function sanitizeName(name) {
  const clean = String(name ?? '').replace(/[<>\r\n]/g, ' ').trim().slice(0, 14);
  return clean === '' ? 'Jogador' : clean;
}
