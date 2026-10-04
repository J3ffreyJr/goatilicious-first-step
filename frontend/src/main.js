import './styles/index.css';

import { ASSETS } from './config/assets.js';
import { renderDecor, preloadAlternateModels } from './modules/scene.js';
import { initTextures } from './modules/textures.js';
import { initFlavorSwitch, preloadFlavorSwitch } from './modules/flavorSwitch.js';
import { initPointer } from './modules/pointer.js';
import { startAnimationLoop, registerDecor } from './modules/animationLoop.js';
import { initBubbles } from './modules/bubbles.js';
import { initMenu } from './modules/menu.js';
import { whenIdle } from './modules/idle.js';

const modelViewer = document.querySelector('#product-model');

// Plain-DOM features first: they don't need the 3D runtime.
initMenu();
initPointer();
initBubbles();

// model-viewer (~290 KB gzipped, includes three.js) is a separate chunk whose
// evaluation is one long main-thread task. Let the headline (the LCP element)
// paint in its real font first; cap the wait so the can never stalls.
await Promise.race([
  document.fonts.load('1em Galada'),
  new Promise((r) => setTimeout(r, 1500)),
]).catch(() => {});
// Yield once so the swapped-in font can paint. Not requestAnimationFrame:
// it never fires in background tabs, which would stall the can indefinitely.
await new Promise((r) => setTimeout(r, 0));

const { ModelViewerElement } = await import('@google/model-viewer');

// Models are Meshopt-compressed (see scripts/optimize-assets.mjs). model-viewer
// already bundles three's MeshoptDecoder but only enables it after loading a
// script from this location; an empty script turns it on with no extra request.
ModelViewerElement.meshoptDecoderLocation = 'data:text/javascript,';

// The can's src is assigned only now, after the decoder is configured
// (its download already started via <link rel="preload"> in index.html).
modelViewer.src = modelViewer.dataset.src;

initFlavorSwitch(modelViewer);
initTextures(modelViewer, [ASSETS.blueTexture]);
startAnimationLoop(modelViewer);

// The can is the hero; decorative models wait until it has loaded so they
// don't compete with it for bandwidth and main-thread time.
let decorRendered = false;
const loadDecor = () => {
  if (decorRendered) return;
  decorRendered = true;
  whenIdle(() => {
    const { berries, leaves } = renderDecor();
    registerDecor(berries, leaves);
    preloadAlternateModels();
    preloadFlavorSwitch();
  });
};

modelViewer.addEventListener('load', loadDecor, { once: true });
modelViewer.addEventListener('error', loadDecor, { once: true });
