import { FLAVORS } from '../config/scene.js';
import { state, berryState } from '../state.js';
import { setFlavorTexture } from './textures.js';

// GSAP is only needed once a flavor is picked, so it's loaded on demand
let gsap;
const loadGsap = () => import('gsap').then((m) => (gsap = m.default));

/** Warms the GSAP chunk so the first switch starts instantly. */
export function preloadFlavorSwitch() {
  loadGsap();
}

function animateBackground(colors) {
  gsap.to(document.body, {
    '--bg-inner': colors.inner,
    '--bg-mid': colors.mid,
    '--bg-outer': colors.outer,
    duration: 1.5,
    ease: 'power2.inOut',
  });
}

function setBodyTheme(flavor) {
  Object.values(FLAVORS).forEach((f) => {
    if (f.bodyClass) document.body.classList.remove(f.bodyClass);
  });
  if (flavor.bodyClass) document.body.classList.add(flavor.bodyClass);
}

/** 720° can spin with motion blur; swaps theme + texture at the 360° peak. */
async function spinCan(modelViewer, flavor) {
  const spin = { val: 0, blur: 0 };
  const onUpdate = () => {
    state.switchSpin = spin.val;
    modelViewer.style.filter = `blur(${spin.blur}px)`;
  };

  await gsap.to(spin, { val: 360, blur: 15, duration: 0.6, ease: 'power2.in', onUpdate });

  setBodyTheme(flavor);
  await setFlavorTexture(modelViewer, flavor.texture);

  await gsap.to(spin, { val: 720, blur: 0, duration: 1.5, ease: 'back.out(0.7)', onUpdate });
  state.switchSpin = 0;
  modelViewer.style.filter = 'none';
}

/** Implodes the berries into the can, swaps their model, explodes to new positions. */
function swapBerries(modelViewer, flavor) {
  const heroCenter = document.querySelector('.hero-center');
  const canRect = modelViewer.getBoundingClientRect();
  const canX = canRect.left + canRect.width / 2;
  const canY = canRect.top + canRect.height / 2;

  return Promise.all(
    [...berryState].map(([berry, s]) => {
      // Offset from the berry's untransformed layout box to the can's center
      const parentRect = berry.offsetParent?.getBoundingClientRect() ?? { left: 0, top: 0 };
      const centerX = canX - parentRect.left - berry.offsetLeft - berry.offsetWidth / 2;
      const centerY = canY - parentRect.top - berry.offsetTop - berry.offsetHeight / 2;

      const startAngle = s.angle;
      const nextBaseX = (Math.random() - 0.5) * 200;
      const nextBaseY = (Math.random() - 0.5) * 200;

      gsap.set(berry, { rotation: startAngle, x: s.baseX, y: s.baseY });

      return gsap
        .timeline()
        .to(berry, {
          x: centerX,
          y: centerY,
          rotation: startAngle + 45,
          scale: 0.1,
          opacity: 0,
          duration: 0.5,
          ease: 'power2.in',
          onComplete: () => {
            berry.src = flavor.berryModel;
            heroCenter.style.zIndex = 50;
          },
        })
        .to(berry, { duration: 0.3 })
        .to(berry, {
          onStart: () => {
            heroCenter.style.zIndex = 1;
          },
          x: nextBaseX,
          y: nextBaseY,
          rotation: startAngle + 90,
          scale: 1,
          opacity: 1,
          duration: 0.9,
          ease: 'back.out(1.5)',
          onComplete: () => {
            Object.assign(s, { angle: startAngle + 90, baseX: nextBaseX, baseY: nextBaseY, rx: 0, ry: 0 });
            // Hand transforms back to the render loop
            gsap.set(berry, { clearProps: 'transform,opacity' });
          },
        })
        .then();
    })
  );
}

async function switchFlavor(modelViewer, key) {
  if (state.isSwitching) return;
  state.isSwitching = true;
  const flavor = FLAVORS[key];

  try {
    if (!gsap) await loadGsap();
    animateBackground(flavor.colors);
    await Promise.all([spinCan(modelViewer, flavor), swapBerries(modelViewer, flavor)]);
  } finally {
    state.isSwitching = false;
  }
}

export function initFlavorSwitch(modelViewer) {
  const cards = [...document.querySelectorAll('.card')];

  const select = (card) => {
    if (state.isSwitching || card.classList.contains('active')) return;
    cards.forEach((c) => {
      c.classList.toggle('active', c === card);
      c.setAttribute('aria-pressed', String(c === card));
    });
    switchFlavor(modelViewer, card.dataset.flavor);
  };

  cards.forEach((card) => card.addEventListener('click', () => select(card)));

  document.querySelectorAll('.nav-arrow').forEach((btn) => {
    btn.addEventListener('click', () => {
      const current = cards.findIndex((c) => c.classList.contains('active'));
      const step = Number(btn.dataset.step);
      select(cards[(current + step + cards.length) % cards.length]);
    });
  });
}
