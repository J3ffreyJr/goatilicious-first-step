import { MOTION } from '../config/scene.js';
import { state, mouse, currentMouse, berryState, reducedMotion } from '../state.js';

const { repulsion, berryFloat, leafFloat, parallax, can } = MOTION;

let berries = [];
let leaves = [];

/** Hands the decorative models (created after the can loads) to the loop. */
export function registerDecor(berryEls, leafEls) {
  berries = berryEls;
  leaves = leafEls;
}

function updateBerry(berry, rect, i, time, still) {
  const s = berryState.get(berry);
  const diffX = mouse.px - (rect.left + rect.width / 2);
  const diffY = mouse.py - (rect.top + rect.height / 2);
  const distance = Math.hypot(diffX, diffY);

  let targetRx = 0;
  let targetRy = 0;
  let speedMult = 1;

  if (distance > 0 && distance < repulsion.radius) {
    const force = (repulsion.radius - distance) / repulsion.radius;
    targetRx = (diffX / distance) * force * repulsion.strength;
    targetRy = (diffY / distance) * force * repulsion.strength;
    speedMult = 1 + force * repulsion.spinBoost;
  }

  s.rx += (targetRx - s.rx) * repulsion.lerp;
  s.ry += (targetRy - s.ry) * repulsion.lerp;
  if (!still) s.angle += repulsion.spinBase * speedMult;

  const dur = berryFloat.durations[i % berryFloat.durations.length];
  const phase = (time + i * 0.7) * ((Math.PI * 2) / dur);
  const floatY = still ? 0 : Math.sin(phase) * berryFloat.amplitude;
  const floatAngle = still ? 0 : Math.cos(phase) * berryFloat.angle;

  berry.style.transform =
    `translate(${s.rx + s.baseX}px, ${s.ry + s.baseY + floatY}px) rotate(${s.angle + floatAngle}deg)`;
}

function updateLeaf(leaf, i, time) {
  const dur = 10 + i * 2;
  const phase = (time + i * 1.2) * ((Math.PI * 2) / dur);
  const floatY = Math.sin(phase) * leafFloat.amplitudeY;
  const floatX = Math.cos(phase * 0.5) * leafFloat.amplitudeX;
  const floatAngle = Math.sin(phase * 0.3) * leafFloat.angle;
  leaf.style.transform = `translate(${floatX}px, ${floatY}px) rotate(${floatAngle}deg)`;
}

/** Per-frame loop: can tilt, parallax layers, berry repulsion/float, leaf float. */
export function startAnimationLoop(modelViewer) {
  const layers = [
    [document.querySelector('.berries-container'), parallax.berriesFG],
    [document.querySelector('.berries-container-bg'), parallax.berriesBG],
    [document.querySelector('.leaves-container'), parallax.leaves],
  ];
  let lastOrbit = '';

  function frame(now) {
    const time = now * 0.001;
    const still = reducedMotion.matches;
    currentMouse.x += (mouse.x - currentMouse.x) * MOTION.mouseLerp;
    currentMouse.y += (mouse.y - currentMouse.y) * MOTION.mouseLerp;

    // Read phase: all layout reads before any style writes (one layout per frame)
    const rects = state.isSwitching ? null : berries.map((b) => b.getBoundingClientRect());

    // Write phase
    const theta = (currentMouse.x * can.tiltX + state.switchSpin).toFixed(2);
    const phi = (can.phi + currentMouse.y * can.tiltY).toFixed(2);
    const orbit = `${theta}deg ${phi}deg ${can.distance}`;
    if (orbit !== lastOrbit) {
      // Only touch the camera when it actually moves; each change triggers a WebGL render
      modelViewer.cameraOrbit = orbit;
      lastOrbit = orbit;
    }

    for (const [el, mult] of layers) {
      el.style.transform = `translate(${currentMouse.x * mult}px, ${currentMouse.y * mult}px)`;
    }

    // GSAP owns berry transforms while a flavor switch is running
    if (rects) {
      berries.forEach((b, i) => {
        if (rects[i].width) updateBerry(b, rects[i], i, time, still); // width 0 → hidden by CSS
      });
    }
    if (!still) leaves.forEach((l, i) => updateLeaf(l, i, time));

    requestAnimationFrame(frame);
  }

  requestAnimationFrame(frame);
}
