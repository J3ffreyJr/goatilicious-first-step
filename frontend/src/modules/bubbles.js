import { ASSETS } from '../config/assets.js';
import { MOTION, COMPACT_QUERY } from '../config/scene.js';
import { reducedMotion } from '../state.js';

function createBubble(container) {
  const bubble = document.createElement('img');
  bubble.src = ASSETS.bubble;
  bubble.className = 'bubble-img';
  bubble.alt = '';
  bubble.decoding = 'async';
  bubble.style.width = `${Math.random() * 20 + 10}px`;
  bubble.style.left = `${Math.random() * 100}%`;
  bubble.style.opacity = Math.random() * 0.4 + 0.2;

  const duration = Math.random() * 6 + 4;
  bubble.style.animationDuration = `${duration}s`;
  bubble.addEventListener('animationend', () => bubble.remove(), { once: true });

  container.appendChild(bubble);
}

export function initBubbles() {
  const container = document.getElementById('bubbles-container');
  if (!container) return;

  // Fewer bubbles on small screens; none while the tab is hidden or motion is reduced
  const interval = window.matchMedia(COMPACT_QUERY).matches
    ? MOTION.bubbles.interval * 2
    : MOTION.bubbles.interval;

  setInterval(() => {
    if (document.hidden || reducedMotion.matches) return;
    createBubble(container);
  }, interval);
}
