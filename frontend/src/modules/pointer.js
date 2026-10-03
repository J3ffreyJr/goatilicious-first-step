import { mouse } from '../state.js';

export function initPointer() {
  window.addEventListener(
    'pointermove',
    (e) => {
      mouse.x = e.clientX / window.innerWidth - 0.5;
      mouse.y = e.clientY / window.innerHeight - 0.5;
      mouse.px = e.clientX;
      mouse.py = e.clientY;
    },
    { passive: true }
  );

  // Stop repelling berries once the pointer leaves the page (or a touch ends)
  const release = () => {
    mouse.px = -9999;
    mouse.py = -9999;
  };
  document.documentElement.addEventListener('pointerleave', release);
  window.addEventListener('pointerup', (e) => e.pointerType !== 'mouse' && release(), { passive: true });
}
