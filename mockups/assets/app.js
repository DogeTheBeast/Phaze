/* ============================================================
   Phaze2 mockups — tiny progressive-enhancement behaviors
   (everything renders correctly with JS disabled)
   ============================================================ */

/* 1. Tabs (library.html) */
document.querySelectorAll('[data-tabgroup]').forEach(function (group) {
  group.querySelectorAll('[data-tab]').forEach(function (btn) {
    btn.addEventListener('click', function () {
      group.querySelectorAll('[data-tab]').forEach(function (b) {
        b.classList.toggle('active', b === btn);
      });
      group.parentElement.querySelectorAll('[data-panel]').forEach(function (p) {
        p.classList.toggle('hidden', p.dataset.panel !== btn.dataset.tab);
      });
    });
  });
});

/* 2. Toggle switches */
document.querySelectorAll('[role="switch"]').forEach(function (el) {
  el.addEventListener('click', function () {
    var on = el.classList.toggle('on');
    el.setAttribute('aria-checked', on ? 'true' : 'false');
  });
});

/* 3. Star buttons: swap star_border <-> star */
var STAR_OUTLINE = 'M22 9.24l-7.19-.62L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21 12 17.27 18.18 21l-1.63-7.03L22 9.24z';
var STAR_FILLED  = 'M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z';
document.querySelectorAll('[data-star]').forEach(function (el) {
  el.addEventListener('click', function () {
    var starred = el.classList.toggle('starred');
    var path = el.querySelector('path');
    if (path) path.setAttribute('d', starred ? STAR_FILLED : STAR_OUTLINE);
  });
});

/* 4. Mini-player play/pause toggle (does NOT navigate) */
document.querySelectorAll('[data-playtoggle]').forEach(function (el) {
  el.addEventListener('click', function (e) {
    e.preventDefault();
    e.stopPropagation();
    var path = el.querySelector('path');
    if (!path) return;
    var playing = el.dataset.playing === '1';
    el.dataset.playing = playing ? '0' : '1';
    path.setAttribute('d', playing
      ? 'M8 5v14l11-7z'                                  /* play */
      : 'M6 19h4V5H6v14zm8-14v14h4V5h-4z');              /* pause */
  });
});

/* 5. Mini-player anchor: don't navigate when clicking inner buttons */
document.querySelectorAll('a.miniplayer').forEach(function (a) {
  a.addEventListener('click', function (e) {
    if (e.target.closest('button')) e.preventDefault();
  });
});
