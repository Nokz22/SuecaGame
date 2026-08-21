import { makeDeck, shuffle, sortHand } from './cards.js?v=__V__';
import { isValidPlay, trickWinner, trickPoints, gamePointsFor } from './rules.js?v=__V__';
import { chooseBotCard } from './bot.js?v=__V__';

export const BOT_NAMES = ['Beatriz', 'Manel', 'Rosa'];
export const POINTS_TO_WIN = 4;

/**
 * Motor do jogo: o humano é sempre o lugar 0, os lugares 0+2 formam a
 * equipa "Nós" e os lugares 1+3 a equipa "Eles". Emite eventos para a UI;
 * quem decide o ritmo (atrasos dos bots, pausas entre vazas) é a UI.
 */
export class Game {
  /**
   * Aceita o nome do jogador humano (lugar 0, contra 3 bots) ou uma lista
   * completa de 4 jogadores [{name, bot}] — usado pelo multiplayer online.
   */
  constructor(playerNameOrPlayers, rng = Math.random) {
    this.rng = rng;
    if (Array.isArray(playerNameOrPlayers)) {
      if (playerNameOrPlayers.length !== 4) {
        throw new Error('São precisos exatamente 4 jogadores');
      }
      this.players = playerNameOrPlayers.map(p => ({ name: p.name, bot: !!p.bot }));
    } else {
      this.players = [
        { name: playerNameOrPlayers || 'Jogador', bot: false },
        { name: BOT_NAMES[0], bot: true },
        { name: BOT_NAMES[1], bot: true },
        { name: BOT_NAMES[2], bot: true },
      ];
    }
    this.gamePoints = [0, 0]; // [nós (0+2), eles (1+3)]
    this.dealer = Math.floor(rng() * 4);
    this.listeners = [];
    this.phase = 'idle';
  }

  on(listener) {
    this.listeners.push(listener);
  }

  emit(event) {
    for (const listener of this.listeners) {
      listener(event);
    }
  }

  startRound() {
    const deck = shuffle(makeDeck(), this.rng);
    this.hands = [[], [], [], []];
    for (let i = 1; i <= 4; i++) {
      const seat = (this.dealer + i) % 4;
      this.hands[seat] = deck.slice((i - 1) * 10, i * 10);
      sortHand(this.hands[seat]);
    }
    // o trunfo é a última carta dada, que fica na mão do carteador
    this.trump = deck[39];
    this.roundPoints = [0, 0];
    this.tricksPlayed = 0;
    this.trick = [];
    this.current = (this.dealer + 1) % 4;
    this.phase = 'playing';
    this.emit({ type: 'round-start', trump: this.trump, dealer: this.dealer });
    this.emit({ type: 'turn', seat: this.current });
  }

  legalCards(seat) {
    const leadCard = this.trick.length ? this.trick[0].card : null;
    return this.hands[seat].filter(c => isValidPlay(c, this.hands[seat], leadCard));
  }

  playCard(seat, cardRef) {
    if (this.phase !== 'playing' || seat !== this.current) {
      return false;
    }
    const hand = this.hands[seat];
    const card = hand.find(c => c.id === cardRef.id);
    if (!card) {
      return false;
    }
    const leadCard = this.trick.length ? this.trick[0].card : null;
    if (!isValidPlay(card, hand, leadCard)) {
      this.emit({ type: 'invalid', seat });
      return false;
    }

    hand.splice(hand.indexOf(card), 1);
    this.trick.push({ seat, card });
    this.emit({ type: 'played', seat, card });

    if (this.trick.length === 4) {
      this.#finishTrick();
    } else {
      this.current = (this.current + 1) % 4;
      this.emit({ type: 'turn', seat: this.current });
    }
    return true;
  }

  #finishTrick() {
    const cards = this.trick.map(p => p.card);
    const winnerSeat = this.trick[trickWinner(cards, this.trump.suit.key)].seat;
    const points = trickPoints(cards);
    this.roundPoints[winnerSeat % 2] += points;
    this.tricksPlayed++;
    this.emit({
      type: 'trick', winnerSeat, points, roundPoints: [...this.roundPoints],
    });

    if (this.tricksPlayed === 10) {
      this.#finishRound();
    } else {
      this.trick = [];
      this.current = winnerSeat;
      this.emit({ type: 'turn', seat: winnerSeat });
    }
  }

  #finishRound() {
    this.gamePoints[0] += gamePointsFor(this.roundPoints[0]);
    this.gamePoints[1] += gamePointsFor(this.roundPoints[1]);
    const over = this.gamePoints[0] >= POINTS_TO_WIN || this.gamePoints[1] >= POINTS_TO_WIN;
    this.phase = over ? 'over' : 'between-rounds';
    this.emit({
      type: 'round-end',
      roundPoints: [...this.roundPoints],
      gamePoints: [...this.gamePoints],
      over,
      winnerTeam: over ? (this.gamePoints[0] >= POINTS_TO_WIN ? 0 : 1) : null,
    });
  }

  nextRound() {
    if (this.phase !== 'between-rounds') {
      return;
    }
    this.dealer = (this.dealer + 1) % 4;
    this.startRound();
  }

  isBotTurn() {
    return this.phase === 'playing' && this.players[this.current].bot;
  }

  botPlay() {
    if (!this.isBotTurn()) {
      return false;
    }
    const seat = this.current;
    const card = chooseBotCard(this.hands[seat], this.trick, this.trump.suit.key, seat);
    return this.playCard(seat, card);
  }
}
