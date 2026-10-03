/** Shared mutable runtime state across modules. */
export const state = {
  isSwitching: false,
  switchSpin: 0,
};

/** Normalized (-0.5..0.5) and pixel pointer position. */
export const mouse = { x: 0, y: 0, px: -9999, py: -9999 };

/** Smoothed pointer position used by the render loop. */
export const currentMouse = { x: 0, y: 0 };

/**
 * Per-berry motion state, keyed by element.
 * base: resting offset (changes after each flavor switch)
 * r:    smoothed pointer-repulsion offset
 */
export const berryState = new Map();

export function initBerryState(el) {
  berryState.set(el, { angle: Math.random() * 360, baseX: 0, baseY: 0, rx: 0, ry: 0 });
}

export const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
