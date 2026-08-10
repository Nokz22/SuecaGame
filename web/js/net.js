// Camada WebRTC via PeerJS (broker público gratuito para o "apresentar"
// inicial; depois disso os dados fluem diretamente entre browsers).
// Expõe o mesmo formato de transporte/ligação que os testes usam em
// loopback: { send, onMessage, onClose, close }.

/* global Peer */

const PREFIX = 'sueca-';
// sem O/0/I/1 para o código ser fácil de ditar em voz alta
const ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
const CODE_LENGTH = 4;

function randomCode() {
  let code = '';
  for (let i = 0; i < CODE_LENGTH; i++) {
    code += ALPHABET[Math.floor(Math.random() * ALPHABET.length)];
  }
  return code;
}

export function normalizeCode(raw) {
  return String(raw ?? '').trim().toUpperCase().replace(/[^A-Z0-9]/g, '');
}

function wrapConnection(dataConn) {
  const wrapped = {
    seat: undefined,
    send(obj) {
      if (dataConn.open) {
        dataConn.send(obj);
      } else {
        queue.push(obj);
      }
    },
    onMessage(cb) {
      dataConn.on('data', cb);
    },
    onClose(cb) {
      dataConn.on('close', cb);
      dataConn.on('error', cb);
    },
    close() {
      try {
        dataConn.close();
      } catch {
        // já fechada
      }
    },
  };
  const queue = [];
  dataConn.on('open', () => {
    for (const obj of queue.splice(0)) {
      dataConn.send(obj);
    }
  });
  return wrapped;
}

/**
 * Cria uma sala: regista este browser no broker com o id "sueca-CÓDIGO".
 * Devolve { code, transport } — o transport entrega ligações de convidados.
 */
export function createRoom({ attempts = 5 } = {}) {
  return new Promise((resolve, reject) => {
    let tries = 0;

    function tryCode() {
      const code = randomCode();
      const peer = new Peer(PREFIX + code);

      peer.on('open', () => {
        resolve({
          code,
          transport: {
            onConnection(cb) {
              peer.on('connection', conn => cb(wrapConnection(conn)));
            },
            close() {
              peer.destroy();
            },
          },
        });
      });

      peer.on('error', err => {
        peer.destroy();
        if (err.type === 'unavailable-id' && ++tries < attempts) {
          tryCode(); // código já em uso: gera outro
        } else {
          reject(new Error(explainError(err)));
        }
      });
    }

    tryCode();
  });
}

/** Liga-se à sala com o código dado; devolve uma ligação pronta a usar. */
export function joinRoom(code, { timeoutMs = 15000 } = {}) {
  return new Promise((resolve, reject) => {
    const peer = new Peer();
    const timer = setTimeout(() => {
      peer.destroy();
      reject(new Error('Não foi possível ligar à sala (tempo esgotado)'));
    }, timeoutMs);

    function fail(message) {
      clearTimeout(timer);
      peer.destroy();
      reject(new Error(message));
    }

    peer.on('open', () => {
      const conn = peer.connect(PREFIX + normalizeCode(code), { serialization: 'json' });
      conn.on('open', () => {
        clearTimeout(timer);
        const wrapped = wrapConnection(conn);
        const close = wrapped.close;
        wrapped.close = () => {
          close();
          peer.destroy();
        };
        resolve(wrapped);
      });
      conn.on('error', err => fail(explainError(err)));
    });

    peer.on('error', err => fail(explainError(err)));
  });
}

function explainError(err) {
  switch (err && err.type) {
    case 'peer-unavailable':
      return 'Sala não encontrada — confirma o código';
    case 'network':
    case 'server-error':
    case 'socket-error':
    case 'socket-closed':
      return 'Sem ligação ao serviço de salas — tenta outra vez';
    case 'browser-incompatible':
      return 'Este browser não suporta WebRTC';
    default:
      return 'Erro de ligação: ' + (err && err.type ? err.type : 'desconhecido');
  }
}
