import { ASSETS } from './assets.js';

/**
 * Flavor themes: background gradient stops, berry model and can texture.
 * `texture: null` means the texture embedded in the can model.
 */
export const FLAVORS = {
  classic: {
    colors: { inner: '#b8823b', mid: '#4a2817', outer: '#1c0e07' },
    berryModel: ASSETS.cherry,
    texture: null,
  },
  blue: {
    colors: { inner: '#8e2b58', mid: '#3a1024', outer: '#14050d' },
    berryModel: ASSETS.blueberry,
    texture: ASSETS.blueTexture,
    bodyClass: 'blue-theme',
  },
};

/** Breakpoint where the layout stacks and decorative models are trimmed (keep in sync with responsive.css). */
export const COMPACT_QUERY = '(max-width: 900px)';

/** Floating leaves in the far background. */
export const LEAVES = [
  { className: 'l1', orbit: '45deg 75deg 105%' },
  { className: 'l2', orbit: '-30deg 60deg 105%' },
  { className: 'l3', orbit: '120deg 85deg 105%' },
  { className: 'l4', orbit: '10deg 45deg 105%' },
];

/** Berries rendered behind the can. */
export const BERRIES_BG = [
  { className: 'b7', orbit: '-20deg 110deg 105%' },
  { className: 'b8', orbit: '160deg 45deg 105%' },
  { className: 'b9', orbit: '45deg 20deg 105%' },
];

/** Berries rendered above the text and can. */
export const BERRIES_FG = [
  { className: 'b1', orbit: '45deg 120deg 105%' },
  { className: 'b2', orbit: '-120deg 45deg 105%' },
  { className: 'b3', orbit: '200deg 90deg 105%' },
  { className: 'b4', orbit: '10deg 20deg 105%' },
  { className: 'b5', orbit: '-45deg 160deg 105%' },
  { className: 'b6', orbit: '80deg 75deg 105%' },
];

export const MOTION = {
  mouseLerp: 0.05,
  can: { tiltX: 40, tiltY: 20, distance: '380%' },
  parallax: { berriesFG: 60, berriesBG: -30, leaves: -15 },
  repulsion: { radius: 400, strength: -80, lerp: 0.1, spinBase: 0.2, spinBoost: 5 },
  berryFloat: { amplitude: 15, angle: 6, durations: [5, 7, 6, 8, 5.5, 6.5, 9, 11, 10] },
  leafFloat: { amplitudeY: 20, amplitudeX: 15, angle: 15 },
  bubbles: { interval: 400 },
};
