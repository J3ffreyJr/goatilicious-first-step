/** Mobile navigation toggle (the nav is always visible on desktop). */
export function initMenu() {
  const toggle = document.querySelector('.nav-toggle');
  const nav = document.getElementById('site-nav');
  if (!toggle || !nav) return;

  const setOpen = (open) => {
    nav.classList.toggle('is-open', open);
    toggle.setAttribute('aria-expanded', String(open));
    toggle.setAttribute('aria-label', open ? 'Close menu' : 'Open menu');
  };

  toggle.addEventListener('click', () => setOpen(!nav.classList.contains('is-open')));

  nav.addEventListener('click', (e) => {
    const link = e.target.closest('.nav-item');
    if (!link) return;
    nav.querySelectorAll('.nav-item').forEach((a) => a.classList.toggle('active', a === link));
    setOpen(false);
  });

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') setOpen(false);
  });

  document.addEventListener('click', (e) => {
    if (!nav.contains(e.target) && !toggle.contains(e.target)) setOpen(false);
  });
}
