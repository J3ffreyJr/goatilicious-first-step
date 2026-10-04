/** Runs `fn` when the main thread is idle (falls back to a timeout on Safari). */
export function whenIdle(fn, timeout = 2000) {
  if ('requestIdleCallback' in window) {
    window.requestIdleCallback(() => fn(), { timeout });
  } else {
    setTimeout(fn, 200);
  }
}
