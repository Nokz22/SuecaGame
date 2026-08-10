import { Game, BOT_NAMES } from './game.js';
import { cardFromId } from './cards.js';
import { encodeEvent, decodeEvent, sanitizeName } from './protocol.js';

// A UI fala sempre com uma sessão e não sabe se o jogo é local, hospedado
// por este browser ou remoto. Todas entregam o mesmo fluxo de eventos,
// já filtrado para este jogador (só a própria mão, só os próprios erros).
//
// Interface comum:
//   mySeat, isHost, names {seat: nome}
//   onEvent(cb), start(), playCard(card), close()

const DEFAULT_BOT_DELAY = 800;

// O evento de fecho do WebRTC não é fiável quando o outro lado morre
// abruptamente (crash do separador, perda de rede). O heartbeat garante
// a deteção: o anfitrião faz ping, os convidados respondem; silêncio
// prolongado de qualquer um dos lados conta como queda.
const DEFAULT_HEARTBEAT_MS = 4000;
const DEFAULT_DROP_AFTER_MS = 15000;

/** Single player: humano no lugar 0 contra 3 bots, tudo neste browser. */
export class LocalSession {
  constructor(playerName, { demo = false, botDelay = DEFAULT_BOT_DELAY } = {}) {
    this.mySeat = 0;
    this.isHost = true;
    this.botDelay = botDelay;
    this.game = new Game(playerName);
    if (demo) {
      this.game.players[0].bot = true;
    }
    this.names = Object.fromEntries(this.game.players.map((p, s) => [s, p.name]));
    this.cb = null;
    this.game.on(e => this.#route(e));
  }

  onEvent(cb) {
    this.cb = cb;
    this.#deliver({ type: 'roster', names: { ...this.names } });
  }

  start() {
    this.game.startRound();
  }

  playCard(card) {
    this.game.playCard(this.mySeat, card);
  }

  #route(event) {
    if (event.type === 'invalid' && event.seat !== this.mySeat) {
      return;
    }
    this.#deliver(event);
    if (event.type === 'round-start') {
      this.#deliver({ type: 'hand', seat: this.mySeat, cards: [...this.game.hands[this.mySeat]] });
    }
    if (event.type === 'turn' && this.game.isBotTurn()) {
      setTimeout(() => this.game.botPlay(), this.botDelay);
    }
    if (event.type === 'round-end' && !event.over) {
      // dá tempo à UI de mostrar o resumo; quem carrega no botão chama nextRound
    }
  }

  nextRound() {
    this.game.nextRound();
  }

  #deliver(event) {
    if (this.cb) {
      this.cb(event);
    }
  }

  close() {
    this.game = null;
  }
}

/**
 * Anfitrião online: este browser é a autoridade do jogo. Aceita convidados
 * pelo transporte (WebRTC via PeerJS no browser, loopback nos testes),
 * atribui lugares por ordem de chegada e envia a cada um apenas o que
 * lhe diz respeito. Convidado que sai a meio vira bot.
 */
export class HostSession {
  constructor(playerName, transport, {
    botDelay = DEFAULT_BOT_DELAY,
    heartbeatMs = DEFAULT_HEARTBEAT_MS,
    dropAfterMs = DEFAULT_DROP_AFTER_MS,
  } = {}) {
    this.mySeat = 0;
    this.isHost = true;
    this.botDelay = botDelay;
    this.dropAfterMs = dropAfterMs;
    this.transport = transport;
    this.names = { 0: sanitizeName(playerName) };
    this.guests = {}; // seat -> connection
    this.game = null;
    this.cb = null;
    transport.onConnection(conn => this.#accept(conn));
    this.heartbeatTimer = setInterval(() => this.#heartbeat(), heartbeatMs);
  }

  #heartbeat() {
    for (const conn of Object.values(this.guests)) {
      if (Date.now() - conn.lastSeen > this.dropAfterMs) {
        conn.close();
        this.#onGuestGone(conn);
      } else {
        conn.send({ t: 'ping' });
      }
    }
  }

  onEvent(cb) {
    this.cb = cb;
    this.#deliverLocal({ type: 'roster', names: { ...this.names } });
  }

  start() {
    if (this.game) {
      return;
    }
    const players = [];
    let botIndex = 0;
    for (let seat = 0; seat < 4; seat++) {
      if (this.names[seat] !== undefined) {
        players.push({ name: this.names[seat], bot: false });
      } else {
        const botName = BOT_NAMES[botIndex++ % BOT_NAMES.length];
        this.names[seat] = botName;
        players.push({ name: botName, bot: true });
      }
    }
    this.game = new Game(players);
    this.game.on(e => this.#route(e));
    this.#broadcastRoster();
    this.game.startRound();
  }

  playCard(card) {
    if (this.game) {
      this.game.playCard(this.mySeat, card);
    }
  }

  nextRound() {
    if (this.game) {
      this.game.nextRound();
    }
  }

  #accept(conn) {
    conn.lastSeen = Date.now();
    conn.onMessage(msg => {
      conn.lastSeen = Date.now();
      this.#onGuestMessage(conn, msg);
    });
    conn.onClose(() => this.#onGuestGone(conn));
  }

  #freeSeat() {
    for (let seat = 1; seat < 4; seat++) {
      if (this.guests[seat] === undefined && this.names[seat] === undefined) {
        return seat;
      }
    }
    return -1;
  }

  #onGuestMessage(conn, msg) {
    if (!msg || typeof msg !== 'object') {
      return;
    }
    if (msg.t === 'join') {
      if (this.game || conn.seat !== undefined) {
        conn.send({ t: 'full' });
        conn.close();
        return;
      }
      const seat = this.#freeSeat();
      if (seat < 0) {
        conn.send({ t: 'full' });
        conn.close();
        return;
      }
      conn.seat = seat;
      this.guests[seat] = conn;
      this.names[seat] = this.#uniqueName(sanitizeName(msg.name));
      conn.send({ t: 'welcome', seat, names: { ...this.names } });
      this.#broadcastRoster();
      return;
    }
    if (msg.t === 'play' && this.game && conn.seat !== undefined) {
      try {
        this.game.playCard(conn.seat, cardFromId(msg.id));
      } catch {
        // id malformado: ignora — o motor nunca vê a jogada
      }
    }
    // 'pong' não precisa de tratamento: qualquer mensagem renova o lastSeen
  }

  #uniqueName(name) {
    const taken = Object.values(this.names);
    let candidate = name;
    let n = 2;
    while (taken.includes(candidate)) {
      candidate = `${name} ${n++}`;
    }
    return candidate;
  }

  #onGuestGone(conn) {
    const seat = conn.seat;
    if (seat === undefined || this.guests[seat] !== conn) {
      return;
    }
    delete this.guests[seat];
    if (!this.game) {
      delete this.names[seat];
      this.#broadcastRoster();
      return;
    }
    // a meio do jogo: o lugar passa a bot e a partida continua
    this.game.players[seat].bot = true;
    this.names[seat] = `${this.names[seat]} (bot)`;
    this.#broadcastRoster();
    if (this.game.isBotTurn()) {
      setTimeout(() => this.game && this.game.botPlay(), this.botDelay);
    }
  }

  #route(event) {
    switch (event.type) {
      case 'invalid':
        if (event.seat === this.mySeat) {
          this.#deliverLocal(event);
        } else {
          this.#sendToSeat(event.seat, event);
        }
        break;
      case 'round-start': {
        this.#deliverAll(event);
        this.#deliverLocal({ type: 'hand', seat: 0, cards: [...this.game.hands[0]] });
        for (let seat = 1; seat < 4; seat++) {
          this.#sendToSeat(seat, { type: 'hand', seat, cards: [...this.game.hands[seat]] });
        }
        break;
      }
      default:
        this.#deliverAll(event);
    }
    if (event.type === 'turn' && this.game.isBotTurn()) {
      setTimeout(() => this.game && this.game.botPlay(), this.botDelay);
    }
  }

  #broadcastRoster() {
    this.#deliverAll({ type: 'roster', names: { ...this.names } });
  }

  #deliverAll(event) {
    this.#deliverLocal(event);
    const wire = { t: 'event', e: encodeEvent(event) };
    for (const conn of Object.values(this.guests)) {
      conn.send(wire);
    }
  }

  #sendToSeat(seat, event) {
    const conn = this.guests[seat];
    if (conn) {
      conn.send({ t: 'event', e: encodeEvent(event) });
    }
  }

  #deliverLocal(event) {
    if (this.cb) {
      this.cb(event);
    }
  }

  close() {
    clearInterval(this.heartbeatTimer);
    for (const conn of Object.values(this.guests)) {
      conn.close();
    }
    this.transport.close();
    this.game = null;
  }
}

/** Convidado online: liga-se ao anfitrião e limita-se a reagir a eventos. */
export class GuestSession {
  static join(playerName, connection, { timeoutMs = 10000, ...opts } = {}) {
    return new Promise((resolve, reject) => {
      const session = new GuestSession(connection, opts);
      const timer = setTimeout(() => {
        session.close();
        reject(new Error('O anfitrião não respondeu'));
      }, timeoutMs);

      session.onWelcome = () => {
        clearTimeout(timer);
        resolve(session);
      };
      session.onRejected = reason => {
        clearTimeout(timer);
        session.close();
        reject(new Error(reason));
      };
      connection.send({ t: 'join', name: sanitizeName(playerName) });
    });
  }

  constructor(connection, {
    heartbeatMs = DEFAULT_HEARTBEAT_MS,
    dropAfterMs = DEFAULT_DROP_AFTER_MS,
  } = {}) {
    this.conn = connection;
    this.mySeat = -1;
    this.isHost = false;
    this.names = {};
    this.cb = null;
    this.pending = [];
    this.onWelcome = null;
    this.onRejected = null;
    this.left = false;
    this.lastSeenHost = Date.now();
    connection.onMessage(msg => {
      this.lastSeenHost = Date.now();
      this.#onMessage(msg);
    });
    connection.onClose(() => this.#hostGone());
    this.heartbeatTimer = setInterval(() => {
      if (Date.now() - this.lastSeenHost > dropAfterMs) {
        this.conn.close();
        this.#hostGone();
      }
    }, heartbeatMs);
  }

  #hostGone() {
    if (this.left) {
      return;
    }
    this.left = true;
    clearInterval(this.heartbeatTimer);
    this.#deliver({ type: 'host-left' });
  }

  onEvent(cb) {
    this.cb = cb;
    for (const event of this.pending.splice(0)) {
      cb(event);
    }
  }

  start() {
    // só o anfitrião começa o jogo
  }

  playCard(card) {
    this.conn.send({ t: 'play', id: card.id });
  }

  nextRound() {
    // o anfitrião avança as rondas
  }

  #onMessage(msg) {
    if (!msg || typeof msg !== 'object') {
      return;
    }
    if (msg.t === 'ping') {
      this.conn.send({ t: 'pong' });
      return;
    }
    if (msg.t === 'welcome') {
      this.mySeat = msg.seat;
      this.names = { ...msg.names };
      this.#deliver({ type: 'roster', names: { ...this.names } });
      if (this.onWelcome) {
        this.onWelcome();
      }
      return;
    }
    if (msg.t === 'full') {
      if (this.onRejected) {
        this.onRejected('A sala já está cheia');
      }
      return;
    }
    if (msg.t === 'event') {
      const event = decodeEvent(msg.e);
      if (event.type === 'roster') {
        this.names = { ...event.names };
      }
      this.#deliver(event);
    }
  }

  #deliver(event) {
    if (this.cb) {
      this.cb(event);
    } else {
      this.pending.push(event);
    }
  }

  close() {
    this.left = true; // saída voluntária: não é "o anfitrião saiu"
    clearInterval(this.heartbeatTimer);
    this.conn.close();
  }
}
