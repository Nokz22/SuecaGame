// Faces de cartas desenhadas à mão: pips clássicos para as cartas numéricas,
// medalhões ornamentados para as figuras. Tudo DOM + SVG inline, sem imagens.
import { cardLabel } from './cards.js';

// posições [x%, y%, rodado?] seguindo os arranjos clássicos de baralho
const PIP_LAYOUTS = {
  DOIS: [[50, 24], [50, 76, 1]],
  TRES: [[50, 20], [50, 50], [50, 80, 1]],
  QUATRO: [[31, 24], [69, 24], [31, 76, 1], [69, 76, 1]],
  CINCO: [[31, 22], [69, 22], [50, 50], [31, 78, 1], [69, 78, 1]],
  SEIS: [[31, 22], [69, 22], [31, 50], [69, 50], [31, 78, 1], [69, 78, 1]],
  SETE: [[31, 22], [69, 22], [50, 36], [31, 50], [69, 50], [31, 78, 1], [69, 78, 1]],
};

// coroas e emblemas das figuras (paths simples, cor herdada do naipe)
const COURT_ICONS = {
  REI: '<path d="M4 15 L6 6 L10 11 L14 4 L18 11 L22 6 L24 15 Z" fill="currentColor"/>'
    + '<rect x="4" y="16.5" width="20" height="3.5" rx="1.2" fill="currentColor"/>',
  DAMA: '<path d="M14 3 L16.6 9.4 L23.5 10 L18.4 14.6 L20 21.5 L14 17.8 L8 21.5 L9.6 14.6 L4.5 10 L11.4 9.4 Z" fill="currentColor"/>',
  VALETE: '<path d="M14 3 L23 6 V13 C23 18.5 19.3 22.3 14 24.5 C8.7 22.3 5 18.5 5 13 V6 Z" fill="currentColor"/>'
    + '<path d="M14 7 L19.5 8.8 V13 C19.5 16.4 17.4 18.9 14 20.6 Z" fill="#fdfbf4"/>',
};

const COURT_TITLES = { REI: 'Rei', DAMA: 'Dama', VALETE: 'Valete' };

export function buildFace(card) {
  const face = document.createElement('span');
  face.className = 'face';
  const key = card.value.key;

  if (PIP_LAYOUTS[key]) {
    for (const [x, y, flipped] of PIP_LAYOUTS[key]) {
      const pip = document.createElement('i');
      pip.className = 'pip' + (flipped ? ' flip' : '');
      pip.style.left = x + '%';
      pip.style.top = y + '%';
      pip.textContent = card.suit.symbol;
      face.append(pip);
    }
  } else if (key === 'AS') {
    const pip = document.createElement('i');
    pip.className = 'pip ace';
    pip.textContent = card.suit.symbol;
    face.append(pip);
  } else {
    face.append(courtMedallion(card));
  }
  return face;
}

function courtMedallion(card) {
  const box = document.createElement('span');
  box.className = 'court';
  box.innerHTML =
    `<svg class="court-icon" viewBox="0 0 28 26" aria-hidden="true">${COURT_ICONS[card.value.key]}</svg>` +
    `<b class="court-letter">${card.value.short}</b>` +
    `<i class="court-suit">${card.suit.symbol}</i>`;
  const title = document.createElement('u');
  title.className = 'court-title';
  title.textContent = COURT_TITLES[card.value.key];
  box.append(title);
  return box;
}

/** Elemento completo de carta (botão) com cantos, moldura interior e face. */
export function cardElement(card) {
  const el = document.createElement('button');
  el.type = 'button';
  el.className = 'card ' + (card.suit.red ? 'red' : 'black');
  el.dataset.id = card.id;
  el.setAttribute('aria-label', cardLabel(card));

  for (const corner of ['tl', 'br']) {
    const c = document.createElement('span');
    c.className = 'corner ' + corner;
    c.innerHTML = `${card.value.short}<i>${card.suit.symbol}</i>`;
    el.append(c);
  }
  const frame = document.createElement('span');
  frame.className = 'frame';
  el.append(frame, buildFace(card));
  return el;
}
