import { whenIdle } from './idle.js';

const cache = new Map();
let embedded = null;
/** URL of the texture the current flavor wants (`null` = embedded). */
let wanted = null;

/** Sets the base-color texture on every material of the model that has one. */
function applyTexture(modelViewer, texture) {
  if (!modelViewer.model || !texture) return;
  modelViewer.model.materials.forEach((material) => {
    const info = material.pbrMetallicRoughness.baseColorTexture;
    // Only the label material has a texture; the lid keeps its plain color.
    if (info?.texture) info.setTexture(texture);
  });
}

/** Resolves a flavor texture: `null` → the one embedded in the model, otherwise a URL (cached). */
function getTexture(modelViewer, url) {
  if (!url) return Promise.resolve(embedded);
  if (!cache.has(url)) {
    const promise = modelViewer.createTexture(url).catch((e) => {
      cache.delete(url);
      throw e;
    });
    cache.set(url, promise);
  }
  return cache.get(url);
}

/** Paints the lid (the material without a label texture) in the flavor's color. */
export function setLidColor(modelViewer, rgba) {
  if (!modelViewer.model || !rgba) return;
  modelViewer.model.materials.forEach((material) => {
    if (!material.pbrMetallicRoughness.baseColorTexture?.texture) {
      material.pbrMetallicRoughness.setBaseColorFactor(rgba);
    }
  });
}

/** Loads (if needed) and applies a flavor texture. */
export async function setFlavorTexture(modelViewer, url) {
  wanted = url;
  try {
    const texture = await getTexture(modelViewer, url);
    if (wanted === url) applyTexture(modelViewer, texture); // ignore stale loads
  } catch (e) {
    console.error('Texture load failed', e);
  }
}

/**
 * Once the can is loaded: keep a handle on its embedded texture, then — when the
 * browser is idle — fetch the alternate textures and warm up their shaders so the
 * first flavor switch doesn't hitch.
 */
export function initTextures(modelViewer, urls) {
  modelViewer.addEventListener('load', () => {
    const baseColor = modelViewer.model?.materials[0]?.pbrMetallicRoughness.baseColorTexture;
    embedded = baseColor?.texture ?? null;
    if (!baseColor || !embedded) return;

    whenIdle(async () => {
      for (const url of urls) {
        try {
          const texture = await getTexture(modelViewer, url);
          baseColor.setTexture(texture);
          await new Promise((r) => requestAnimationFrame(r));
          // Restore whatever the current flavor wants — a switch may have
          // happened during the warm-up frame.
          applyTexture(modelViewer, await getTexture(modelViewer, wanted));
        } catch (e) {
          console.error('Texture preload failed', e);
        }
      }
    });
  }, { once: true });
}
