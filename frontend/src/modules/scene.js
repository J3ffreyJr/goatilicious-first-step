import { ASSETS } from '../config/assets.js';
import { LEAVES, BERRIES_BG, BERRIES_FG } from '../config/scene.js';
import { initBerryState } from '../state.js';
import { whenIdle } from './idle.js';

function createModel(className, src, orbit, exposure) {
  const el = document.createElement('model-viewer');
  el.className = className;
  el.setAttribute('src', src);
  el.setAttribute('environment-image', 'neutral');
  el.setAttribute('exposure', exposure);
  el.setAttribute('interaction-prompt', 'none');
  el.setAttribute('camera-orbit', orbit);
  return el; // containers are `inert`: no focus, hidden from assistive tech
}

function build(container, items, baseClass, src, exposure) {
  return items.map(({ className, orbit }) => ({
    container,
    el: createModel(`${baseClass} ${className}`, src, orbit, exposure),
  }));
}

/**
 * Creates the decorative leaves/berries. Elements are attached one per idle
 * callback: each model-viewer instance compiles shaders and uploads textures,
 * and doing them all at once produces one long main-thread task.
 *
 * Returns the berries in DOM order (background first), which drives each
 * berry's float timing; they're safe to animate before they're attached.
 */
export function renderDecor() {
  const leaves = build(document.querySelector('.leaves-container'), LEAVES, 'leaf', ASSETS.leaves, '1.0');
  const berries = [
    ...build(document.querySelector('.berries-container-bg'), BERRIES_BG, 'berry', ASSETS.cherry, '1.0'),
    ...build(document.querySelector('.berries-container'), BERRIES_FG, 'berry', ASSETS.cherry, '1.2'),
  ];

  // Foreground berries first (most visible), then background, then leaves
  const queue = [...berries.slice(BERRIES_BG.length), ...berries.slice(0, BERRIES_BG.length), ...leaves];
  const attachNext = () => {
    const item = queue.shift();
    if (!item) return;
    item.container.append(item.el);
    whenIdle(attachNext, 500);
  };
  attachNext();

  const berryEls = berries.map((b) => b.el);
  berryEls.forEach(initBerryState);
  return { berries: berryEls, leaves: leaves.map((l) => l.el) };
}

/** Loads the alternate berry model into model-viewer's cache ahead of a flavor switch. */
export function preloadAlternateModels() {
  whenIdle(() => {
    const el = document.createElement('model-viewer');
    el.setAttribute('src', ASSETS.blueberry);
    el.setAttribute('loading', 'eager');
    el.inert = true;
    // Not display:none — model-viewer skips loading elements that aren't rendered
    el.className = 'model-preload';
    document.body.append(el);
  }, 4000);
}
