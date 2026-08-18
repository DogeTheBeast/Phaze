# Phaze2 — Mockup Implementation Spec

> **Purpose of this file:** Complete, self-contained context for a coding agent to implement
> the Phaze2 UI mockups. You need no other information. Follow this spec exactly; where it is
> silent, make the simplest reasonable choice and keep it consistent.

---

## 1. Product context

**Phaze2** is an Android music player for **Subsonic-compatible servers** (Navidrome, Airsonic,
Gonic, etc.). Core value props: stream from your own server, and **download music for offline
playback**. These mockups are the design reference for a future Jetpack Compose (Material 3)
app, so every visual decision should map to Material Design 3 concepts.

The mockups are **static HTML/CSS/vanilla-JS**, viewed in a desktop Linux browser. They are a
*design artifact*, not an app — fake data, no networking, no frameworks.

---

## 2. Hard constraints

- **No external resources.** No CDNs, no Google Fonts, no image files, no network requests of
  any kind. Everything must work from `file://` with the network unplugged.
- **Icons:** inline SVG only, from the icon table in §7 (exact path data provided — copy it).
- **Album/artist art:** CSS gradients + initials (see §5.4). No image files.
- **Fonts:** system stack only: `font-family: system-ui, Roboto, "Segoe UI", sans-serif;`
- **JS:** a single `assets/app.js`, vanilla, progressive-enhancement only. Every screen must
  still render correctly with JS disabled.
- **One shared stylesheet** (`assets/app.css`). No per-screen CSS files; page-specific rules
  may live in clearly-commented sections of `app.css`.
- Dark theme only. Pixel-perfect is not required; **consistency is** (same tokens, same
  components, same frame on every screen).
- Relative links only (`../assets/app.css`, `home.html`, …) so the folder is relocatable.

---

## 3. File structure to create

```
mockups/
├── index.html            # gallery: all screens at a glance (see §10)
├── README.md             # how to view + screen/API mapping (see §12)
├── assets/
│   ├── app.css           # full design system, all components (§4–§6)
│   └── app.js            # tabs, toggles, star, mini-player (§11)
└── screens/
    ├── setup.html        # 1  server connection
    ├── home.html         # 2  home / discovery
    ├── library.html      # 3  library (tabs: artists/albums/songs/playlists)
    ├── artist.html       # 4  artist detail
    ├── album.html        # 5  album detail + track list
    ├── player.html       # 6  now playing
    ├── queue.html        # 7  up-next queue
    ├── downloads.html    # 8  downloads & offline
    ├── search.html       # 9  search
    └── settings.html     # 10 settings
```

Each screen is a **standalone page** (opens directly in a browser and looks correct).
`index.html` embeds every screen in scaled-down iframes for the gallery view.

Every HTML file: `<!doctype html>`, `<html lang="en">`, `<meta charset="utf-8">`,
`<meta name="viewport" content="width=device-width, initial-scale=1">`, title format
`Phaze2 — <Screen Name>`.

---

## 4. Design tokens (define verbatim as CSS custom properties on `:root`)

Material 3 dark scheme, purple seed:

```css
--primary: #D0BCFF;            /* buttons, active states, seek bar fill   */
--on-primary: #381E72;
--primary-container: #4F378B;  /* tonal buttons, chips                    */
--on-primary-container: #EADDFF;
--secondary: #CCC2DC;
--secondary-container: #4A4458;
--tertiary: #EFB8C8;
--surface: #141218;
--surface-dim: #0F0D13;        /* app background                          */
--surface-container-lowest: #0F0D13;
--surface-container-low: #1D1B20;
--surface-container: #211F26;  /* cards, sheets, mini-player              */
--surface-container-high: #2B2930;   /* app bar, nav bar                  */
--surface-container-highest: #36343B;
--on-surface: #E6E0E9;
--on-surface-variant: #CAC4D0; /* secondary text                          */
--outline: #938F99;
--outline-variant: #49454F;
--error: #F2B8B5;
--downloaded: #A8DAB5;         /* offline / downloaded accent (greenish)  */
--page-bg: #0B0B10;            /* desktop backdrop around the phone frame */
```

- **Type scale:** display 22px/600 · title 17px/600 · body 14px/400 · label 12px/500
  (letter-spacing 0.3px) · caption 11px/400, `on-surface-variant`.
- **Shape:** `--r-sm: 8px` (chips, small buttons) · `--r-md: 12px` (cards, art) ·
  `--r-lg: 20px` (sheets, dialogs) · full-round for FABs, icon buttons, pill buttons.
- **Spacing:** 4px base grid; screen side padding 16px; card gaps 12px.
- **Elevation:** no heavy shadows; use the tonal surface steps above. Slight
  `box-shadow: 0 4px 16px rgba(0,0,0,.45)` only for the phone frame and the mini-player.

---

## 5. Shared building blocks

### 5.1 Phone frame (use on EVERY screen, and it is what the gallery shows)

Exact wrapper markup — copy this on every screen, changing only the content inside
`.screen` and the active nav state:

```html
<body class="stage">
  <div class="phone">
    <div class="statusbar">
      <span class="time">9:41</span>
      <span class="status-icons"><!-- tiny inline SVGs: wifi, battery -->•</span>
    </div>
    <div class="screen">
      <!-- app bar + scrollable content (+ optional mini-player + bottom nav) -->
    </div>
    <div class="gesturebar"><span class="pill"></span></div>
  </div>
</body>
```

- `.stage`: full-viewport flex center, `background: var(--page-bg)`, subtle radial vignette.
- `.phone`: `width:412px; height:892px; border-radius:36px; overflow:hidden; position:relative;
  background: var(--surface-dim); border:1px solid #2a2a33;` + frame shadow. Flex column.
- `.statusbar`: 28px, flex space-between, 12px caption text, `on-surface-variant`.
- `.screen`: `flex:1; display:flex; flex-direction:column; overflow:hidden; position:relative;`
- `.gesturebar`: 20px, centered `.pill` (110×4px, rounded, `on-surface` at 40% opacity).
- Content area scrolls internally (`overflow-y:auto`), app bar/nav stay fixed. Custom slim
  scrollbar (`::-webkit-scrollbar {width:4px}` thumb `outline-variant`) — nice but optional.

### 5.2 App bar (64px)

Left: back arrow (icon `arrow_back`, links per §9) **or** screen title; title 22px display.
Right: icon buttons (`search` → `search.html`, `settings` → `settings.html` where relevant).
Background `surface-container-high` on scrolled look — static is fine, always use
`surface` with a 1px `outline-variant` bottom border.

### 5.3 Bottom navigation (80px, M3 navigation bar)

Four destinations, each = icon + 12px label, stacked, 64px min width.
Active = pill-shaped `secondary-container` highlight behind icon + `on-surface` icon/label;
inactive = `on-surface-variant`.

| Label     | Icon            | Links to         |
|-----------|-----------------|------------------|
| Home      | `home`          | `home.html`      |
| Library   | `library_music` | `library.html`   |
| Downloads | `download`      | `downloads.html` |
| Settings  | `settings`      | `settings.html`  |

### 5.4 Album / artist art (no images!)

```html
<div class="art" style="--g1:#f6d365;--g2:#fda085"><span>MK</span></div>
```

- `.art`: square, `border-radius: var(--r-md)`,
  `background: linear-gradient(135deg, var(--g1), var(--g2))`, centered `<span>` =
  1–2 initials, 600 weight, `rgba(0,0,0,.55)` color, font-size ≈ 40% of box size.
- Add a subtle top-left sheen: `::after` with `linear-gradient(160deg, rgba(255,255,255,.18), transparent 45%)`.
- Sizes: `.art-card` (rail cards, 148px) · `.art-row` (list rows, 48px) ·
  `.art-hero` (album page, 216px) · `.art-player` (now playing, 320px).
- `.art-circle` variant (border-radius 50%) for artists.
- **Downloaded badge:** absolutely positioned 20px `check_circle` icon,
  `downloaded` color on `surface` circular backdrop, bottom-right of art (class `.dl-badge`).
- **Downloading state:** thin 3px linear progress bar at art bottom (class `.dl-progress`
  with inline `style="--p:62%"`).

### 5.5 Other components (specify once in `app.css`, reuse everywhere)

- **Filled button** (primary bg, on-primary text, pill) · **tonal button**
  (primary-container/on-primary-container) · **icon button** (40px round, on-surface-variant,
  transparent bg).
- **Mini-player** (on home/library/downloads screens, sits directly above bottom nav):
  64px, `surface-container`, rounded 12px, 4px side margin; 40px art, two-line title/artist
  (truncated), `play_arrow` + `skip_next` icon buttons; 2px progress hairline at its top edge
  in `--primary` at 37% width. **Whole mini-player is an `<a>` to `player.html`.**
- **Track row:** [index number | 40px art (optional per screen) | title + artist/duration
  | download-state icon | `more_vert`].
- **Download-state icon:** downloaded = `check_circle` in `--downloaded`; not downloaded =
  `download` in `on-surface-variant`; downloading = caption `%` text in `--primary`.
- **Section header:** 17px title + optional right-aligned "More" text button.
- **Chip:** pill, `outline-variant` border or `secondary-container` fill when active.
- **Toggle switch:** M3 switch, 52×32px track; off = `surface-container-highest` track +
  `outline` border; on = `--primary` track + `--on-primary` thumb. `<button role="switch"
  aria-checked>`; JS toggles `.on`.
- **Seek bar:** 4px rounded track (`surface-container-highest`), `--primary` fill + 12px
  thumb dot; buffered portion = `on-surface` at 20% opacity.
- **Offline banner:** full-width strip, `secondary-container` bg, `cloud_off` icon +
  caption text. Used on `home.html` and `downloads.html`.
- **List rows** (settings/library): 56px min height, leading icon or art, title + optional
  caption, trailing control (switch / chevron / value caption).

---

## 6. Scroll behavior

Keep it simple: `.screen` is a flex column; app bar fixed at top, an
`.content` div with `flex:1; overflow-y:auto; padding:0 16px 16px;`, then mini-player +
bottom nav as fixed siblings. No sticky headers needed.

---

## 7. Icon set — inline SVG, copy these paths verbatim

Use: `<svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor"><path d="…"/></svg>`.
Size via CSS classes (`.i18`=18px, `.i24`=24px, `.i32`=32px, `.i48`=48px).

| name | path `d` |
|---|---|
| play_arrow | `M8 5v14l11-7z` |
| pause | `M6 19h4V5H6v14zm8-14v14h4V5h-4z` |
| skip_next | `M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z` |
| skip_previous | `M6 6h2v12H6zm3.5 6l8.5 6V6z` |
| shuffle | `M10.59 9.17L5.41 4 4 5.41l5.17 5.17 1.42-1.41zM14.5 4l2.04 2.04L4 18.59 5.41 20 17.96 7.46 20 9.5V4h-5.5zm.33 9.41l-1.41 1.41 3.13 3.13L14.5 20H20v-5.5l-2.04 2.04-3.13-3.13z` |
| repeat | `M7 7h10v3l4-4-4-4v3H5v6h2V7zm10 10H7v-3l-4 4 4 4v-3h12v-6h-2v4z` |
| star | `M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z` |
| star_border | `M22 9.24l-7.19-.62L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21 12 17.27 18.18 21l-1.63-7.03L22 9.24z` |
| download | `M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z` |
| check_circle | `M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z` |
| cloud_off | `M19.35 10.04C18.67 6.59 15.64 4 12 4c-1.48 0-2.85.43-4.01 1.17l1.46 1.46C10.21 6.23 11.08 6 12 6c3.04 0 5.5 2.46 5.5 5.5v.5H19c1.66 0 3 1.34 3 3 0 1.13-.64 2.11-1.56 2.62l1.45 1.45C23.16 18.16 24 16.68 24 15c0-2.64-2.05-4.78-4.65-4.96zM3 5.27l2.75 2.74C2.56 8.15 0 10.77 0 14c0 3.31 2.69 6 6 6h11.73l2 2L21 20.73 4.27 4 3 5.27zM7.73 10l8 8H6c-2.21 0-4-1.79-4-4s1.79-4 4-4h1.73z` |
| search | `M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z` |
| settings | `M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z` |
| home | `M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z` |
| library_music | `M20 2H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-2 5h-3v5.5c0 1.38-1.12 2.5-2.5 2.5S10 13.88 10 12.5s1.12-2.5 2.5-2.5c.57 0 1.08.19 1.5.51V5h4v2zM4 6H2v14c0 1.1.9 2 2 2h14v-2H4V6z` |
| queue_music | `M15 6H3v2h12V6zm0 4H3v2h12v-2zM3 16h8v-2H3v2zM17 6v8.18c-.31-.11-.65-.18-1-.18-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3V8h3V6h-5z` |
| more_vert | `M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z` |
| arrow_back | `M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z` |
| chevron_right | `M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z` |
| close | `M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z` |
| check | `M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z` |
| music_note | `M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z` |
| album | `M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 14.5c-2.49 0-4.5-2.01-4.5-4.5S9.51 7.5 12 7.5s4.5 2.01 4.5 4.5-2.01 4.5-4.5 4.5zm0-5.5c-.55 0-1 .45-1 1s.45 1 1 1 1-.45 1-1-.45-1-1-1z` |
| wifi | `M1 9l2 2c4.97-4.97 13.03-4.97 18 0l2-2C16.93 2.93 7.08 2.93 1 9zm8 8l3 3 3-3c-1.65-1.66-4.34-1.66-6 0zm-4-4l2 2c2.76-2.76 7.24-2.76 10 0l2-2C15.14 9.14 8.87 9.14 5 13z` |
| person | `M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z` |
| lock | `M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z` |
| dns (server) | `M20 13H4c-.55 0-1 .45-1 1v6c0 .55.45 1 1 1h16c.55 0 1-.45 1-1v-6c0-.55-.45-1-1-1zM7 19c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zM20 3H4c-.55 0-1 .45-1 1v6c0 .55.45 1 1 1h16c.55 0 1-.45 1-1V4c0-.55-.45-1-1-1zM7 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2z` |
| delete | `M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z` |
| drag_handle | `M20 9H4v2h16V9zM4 15h16v-2H4v2z` |
| cast | `M21 3H3c-1.1 0-2 .9-2 2v3h2V5h18v14h-7v2h7c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM1 18v3h3c0-1.66-1.34-3-3-3zm0-4v2c2.76 0 5 2.24 5 5h2c0-3.87-3.13-7-7-7zm0-4v2c4.97 0 9 4.03 9 9h2c0-6.08-4.93-11-11-11z` |
| lyrics | `M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z` |

---

## 8. Shared fake dataset (use consistently across ALL screens)

Server: `https://music.example.com` · type **Navidrome** · user **doge**.
Storage: **3.2 GB of 8 GB** used by downloads. Offline count: **214 songs**.

| # | Album | Artist | Year | Tracks | Gradient `--g1` / `--g2` | Initials | Download state |
|---|-------|--------|------|--------|--------------------------|----------|----------------|
| 1 | Mordechai | Khruangbin | 2020 | 12 | `#f6d365` / `#fda085` | MK | downloaded |
| 2 | Currents | Tame Impala | 2015 | 13 | `#a18cd1` / `#fbc2eb` | TI | downloaded |
| 3 | Modal Soul | Nujabes | 2005 | 14 | `#667eea` / `#764ba2` | NJ | not downloaded |
| 4 | Helplessness Blues | Fleet Foxes | 2011 | 12 | `#89f7fe` / `#66a6ff` | FF | downloading 62% |
| 5 | In Rainbows | Radiohead | 2007 | 10 | `#f83600` / `#f9d423` | RH | downloaded |
| 6 | Migration | Bonobo | 2017 | 12 | `#0ba360` / `#3cba92` | BM | not downloaded |
| 7 | Carrie & Lowell | Sufjan Stevens | 2015 | 11 | `#c79081` / `#dfa579` | SS | downloaded |
| 8 | Untourable Album | Men I Trust | 2021 | 13 | `#30cfd0` / `#330867` | MI | not downloaded |

Playlists: **Evening Drive** (42 songs) · **Focus** (28 songs) · **Workout** (17 songs).

**Mordechai track list** (album + player + queue screens): Time (You and I) 5:42 ·
The Answer Is 3:46 · Father Bird, Mother Bird 3:05 · If There Is No Question 5:27 ·
Pelota 2:49 · First Class 4:42 · Hold It Up 4:31 · Connaissais de Face 3:38 ·
So We Won't Forget 5:00 · Shida 4:45 · One to Remember 4:32 · Strings of Ataron 3:18.

Now-playing track: **"Time (You and I)" — Khruangbin — Mordechai**, 5:42 total, position
2:07, buffered to ~60%. Quality chip: `FLAC · 1411 kbps` + downloaded state.

---

## 9. Screen-by-screen specs

Back-arrow behavior everywhere: link to the most sensible parent (listed per screen).
Every screen below includes the phone frame from §5.1. Screens with bottom nav:
home, library, downloads, settings. Screens with mini-player: home, library, downloads.

### 9.1 `setup.html` — Server connection (no app bar, no nav)
- Centered column, 24px padding: logo = 72px tonal circle (`primary-container`) with 36px
  `music_note` in `on-primary-container`; title "Phaze2" (display); caption
  "Connect to your Subsonic server".
- Outlined text fields (label floats on top border; pre-fill values): **Server URL** =
  `https://music.example.com` (leading `dns` icon) · **Username** = `doge` (leading
  `person`) · **Password** = `••••••••` (leading `lock`).
- Filled button **Connect** (full width) → links to `home.html`.
- Below it, text button **Test connection**, and under that a success row: `check_circle`
  in `--downloaded` + caption "Connected · Navidrome 0.52.5" (rendered statically, as if
  the test just succeeded).
- Divider, then "Saved servers" section header + one list row: `dns` icon, name
  "Home server", caption `https://music.example.com`, trailing `check` in `--primary`.

### 9.2 `home.html` — Home (app bar + content + mini-player + bottom nav "Home" active)
- App bar: title "Phaze2"; right icons: `search` (→ `search.html`), `settings`
  (→ `settings.html`).
- Offline banner (demonstrates the offline state): `cloud_off` + "Offline mode ·
  214 songs available".
- Greeting: display "Good evening" + caption "Pick up where you left off".
- Four rails, each = section header + horizontal scroll row (`.rail`: `display:flex;
  gap:12px; overflow-x:auto;` cards don't wrap):
  - **Recently added** — albums 1, 4, 8, 3 (card: `.art-card` + title + artist caption).
  - **Most played** — albums 2, 5, 1, 7.
  - **Recently played** — albums 5, 3, 6, 1.
  - **Random picks** — albums 6, 8, 2, 4.
- Every album card anywhere in the app links to `album.html`; show `.dl-badge` /
  `.dl-progress` per the dataset. Enough bottom padding that content clears the mini-player.

### 9.3 `library.html` — Library (app bar "Library" + `search` icon; bottom nav "Library" active)
- Tab row (M3 secondary tabs: 4 text tabs, active = `--primary` text + 3px underline):
  **Artists · Albums · Songs · Playlists**. Artists active by default. JS switches visible
  panel (§11); without JS, only Artists shows (other panels `hidden` class present in markup).
- **Artists panel:** list rows: 48px `.art-circle` initials, name, caption "N albums",
  `chevron_right` trailing; rows link to `artist.html`. Artists (use dataset order):
  Khruangbin (3) · Tame Impala (4) · Nujabes (2) · Fleet Foxes (3) · Radiohead (9) ·
  Bonobo (7) · Sufjan Stevens (6) · Men I Trust (3). Decorative alphabet index rail
  (A·B·F·K·M·N·R·S·T) fixed at right edge, caption size.
- **Albums panel:** 2-column grid of album cards (all 8 dataset albums).
- **Songs panel:** 8 track rows using Mordechai tracks (40px art, title, artist caption,
  duration, download-state icon, `more_vert`).
- **Playlists panel:** 3 rows: 48px rounded `secondary-container` box with `queue_music`
  icon, playlist name, caption "N songs", `chevron_right`.

### 9.4 `artist.html` — Artist detail: **Khruangbin** (back → `library.html`)
- Header: 96px `.art-circle` (initials K, gradient `#f6d365`/`#fda085`), name (display),
  caption "3 albums · 34 songs".
- Action row: filled **Play** (play icon) · tonal **Shuffle** (shuffle icon) · icon button
  `star_border` (JS-toggleable to `star`).
- "Albums" section: 2-col grid with Mordechai (2020) + two invented but consistent extras:
  **Con Todo El Mundo** (2018, gradient `#ff9a9e`/`#fecfef`) and **The Universe Smiles
  Upon You** (2015, gradient `#a1c4fd`/`#c2e9fb`). All link to `album.html`.
- "Similar artists" chips row: Bonobo · Men I Trust · Khun Narin (chips, no links needed —
  `artist.html` is fine).

### 9.5 `album.html` — Album detail: **Mordechai** (back → `home.html`)
- Hero: centered `.art-hero` (with `.dl-badge`), title, artist line as text link →
  `artist.html`, caption "2020 · 12 songs · 47 min".
- Action row (centered, gap 12): filled **Play** · tonal **Shuffle** · icon button
  `star_border` · icon button `check_circle` in `--downloaded` (= downloaded state).
- Track list: numbered rows (index, title, duration right-aligned before download icon +
  `more_vert`). Mark tracks 1, 2, 5, 6 downloaded (`check_circle`, `--downloaded`);
  all others show `download` icon in `on-surface-variant`. Row 1 highlighted subtly
  (`surface-container` bg) as "currently playing".

### 9.6 `player.html` — Now Playing (back/chevron-down → `home.html`; use `arrow_back`)
- Top row: back button · centered caption "PLAYING FROM ALBUM · Mordechai" · `more_vert`.
- Centered `.art-player` with `.dl-badge`.
- Title "Time (You and I)" (display) + "Khruangbin" caption (→ `artist.html`).
- Seek bar: total 5:42, position 2:07 (≈37%), buffered 60%; timestamps at ends (caption).
- Transport row (centered): `shuffle` (inactive tint) · `skip_previous` (32px) ·
  64px filled circle FAB with `pause` (32px, on-primary) · `skip_next` (32px) · `repeat`.
- Bottom row: quality chip (`FLAC · 1411 kbps` + tiny `check_circle`) · `lyrics` icon ·
  `cast` icon · `queue_music` icon → `queue.html`.

### 9.7 `queue.html` — Up Next (presented as full screen; back → `player.html`)
- App bar: title "Up next", caption-style right text "8 songs · 35 min", `more_vert`.
- List: drag_handle · 40px art · title + artist caption · duration · `close` icon button.
- Row 1 = current track (Time (You and I)): `surface-container` bg, `--primary` title,
  replace drag handle with 18px animated equalizer (3 vertical bars, CSS keyframes
  scaleY — static fallback fine).
- Remaining rows: next 7 Mordechai tracks in order.
- Bottom bar (above gesture bar): tonal button **Save as playlist** + filled button
  **Clear queue** (make Clear tonal and Save filled if that reads better visually — pick one).

### 9.8 `downloads.html` — Downloads & Offline (app bar "Downloads" + `search` icon;
  bottom nav "Downloads" active; mini-player present)
- Offline banner at top: `cloud_off` + "Offline mode is on — showing downloaded music".
- Storage card (`surface-container`, `r-lg`, 16px padding): "Storage" title; linear
  progress bar 3.2/8 GB (40%, `--primary`); caption "3.2 GB of 8 GB used · 214 songs".
- "In progress" section: one row — album 4 art (48px, `.dl-progress`), "Helplessness
  Blues", caption "62% · 84 MB", `close` icon button (cancel).
- "Downloaded albums" section: rows for albums 1, 2, 5, 7 (48px art + `.dl-badge`, title,
  artist caption + size (invent: 210–340 MB), trailing `delete` icon button).
- "Downloaded songs" section: 3 rows (individual tracks from Modal Soul & Migration —
  invent titles consistent with artist), each with `delete` icon.
- Settings rows (list rows): switch ON "Auto-download starred songs" (caption
  "New starred songs download on Wi-Fi") · switch OFF "Download on Wi-Fi only"…
  wait — this belongs ON: switch ON "Download on Wi-Fi only".

### 9.9 `search.html` — Search (back → previous screen, use `home.html`)
- App bar replaced by search field: full-width `surface-container-high` rounded field with
  `search` icon, placeholder text "Artists, albums, songs…" rendered as typed query
  **"nujabes"**, `close` icon to clear.
- "Recent searches" chips: nujabes · fleet foxes · khruangbin.
- Results for query, grouped with section headers:
  - **Artists** — one row: Nujabes circle art, "Nujabes", caption "2 albums" → `artist.html`.
  - **Albums** — Modal Soul card row (→ `album.html`) + invented "Spiritual State (2011,
    gradient `#43e97b`/`#38f9d7`)".
  - **Songs** — 3 track rows: Feather 2:57 · Luv(sic) pt3 5:38 · Aruarian Dance 4:09
    (all "Nujabes", with download-state icons: Feather downloaded, others not).

### 9.10 `settings.html` — Settings (app bar "Settings", back arrow → `home.html`;
  bottom nav "Settings" active)
Grouped list rows with small caption group headers:
- **Server** — row: `dns` icon, "Home server", caption "doge@music.example.com ·
  Navidrome 0.52.5", trailing `chevron_right` → `setup.html`. Row: "Test connection",
  trailing caption "OK" in `--downloaded`.
- **Playback** — "Streaming quality (Wi-Fi)" value "Max (FLAC)" · "Streaming quality
  (mobile data)" value "192 kbps" · switch OFF "Gapless playback"… make it ON.
- **Downloads** — "Download quality" value "320 kbps" · switch ON "Download on Wi-Fi
  only" · switch OFF "Auto-download starred songs" · "Storage limit" value "8 GB".
- **Offline** — switch ON "Offline mode" (caption "Only show downloaded music") · row
  "Manage downloads" `chevron_right` → `downloads.html`.
- **Library** — switch ON "Scrobble plays to server" · switch OFF "Show offline banner".
- **Appearance** — "Theme" value "Dark" · accent color row: 5 small circles
  (`#D0BCFF` selected with ring, plus `#A8DAB5`, `#EFB8C8`, `#89F7FE`, `#F6D365`).
- **About** — "Phaze2" caption "Version 0.1.0-mock · Design mockup".

---

## 10. `index.html` — the gallery (viewer entry point)

Desktop-style page (no phone frame): `--page-bg` background, centered header block:
title "Phaze2", caption "Subsonic client — UI mockups · Material 3 dark". Below, a
responsive grid (`repeat(auto-fill, minmax(230px, 1fr))`, gap 28px, max-width 1400px).
Each tile: screen label (label type, on-surface) + a fixed-size wrapper
(227×492px ≈ 412×892 × 0.55) containing `<iframe src="screens/NAME.html" scrolling="no"
loading="lazy">` at natural size, `transform: scale(0.55); transform-origin: top left;
border:0; pointer-events:none;`. The whole tile is wrapped in `<a href="screens/NAME.html">`
so clicking opens the interactive screen full-size. Tile order: setup, home, library,
artist, album, player, queue, downloads, search, settings. Footer caption: "Click any
screen to open it · All screens are self-contained static mockups".

---

## 11. `app.js` behaviors (progressive enhancement; keep tiny, <150 lines)

1. **Tabs** (`library.html`): `[data-tab]` buttons toggle matching `[data-panel]` panels
   (`.hidden` class) and active styling.
2. **Switches**: click on `[role="switch"]` toggles `.on` + `aria-checked`.
3. **Star buttons**: click toggles between `star_border`/`star` path (swap the `<path>`
   `d` attribute) and `--tertiary` color when starred.
4. **Mini-player**: clicking the play/pause icon swaps `play_arrow`/`pause` and does NOT
   navigate (use `event.preventDefault()` since the mini-player is an `<a>`).
5. Nothing else. No router, no state, no storage.

---

## 12. `mockups/README.md` must contain

- How to view: `xdg-open mockups/index.html` (gallery) or
  `cd mockups && python3 -m http.server 8000` → http://localhost:8000 · any static server works.
- File map (§3 condensed).
- One line per screen → the Subsonic API it maps to:
  setup=`ping`/`getLicense`+auth · home rails=`getAlbumList2` (newest/frequent/recent/random)
  · library=`getArtists`/`getAlbumList`/`getSongs`/`getPlaylists` · artist=`getArtist`+`getAlbum`
  · album=`getAlbum` · player=`stream`/`scrobble`/`star` · queue=client-side ·
  downloads=`download`/`getDownloadedSongs` (client cache) · search=`search3` · settings=client prefs.
- Note: mockups are the design reference for a future Jetpack Compose Material 3 app.

---

## 13. Acceptance checklist (self-verify before reporting done)

1. `mockups/` tree matches §3 exactly — 13 files total.
2. Open every screen: correct tokens, phone frame identical, no broken layout at 412×892.
3. `grep -R "http" mockups/` returns only the fake `music.example.com` strings and README
   text — **zero external requests**.
4. All cross-links resolve (setup→home, cards→album, mini-player→player, bottom nav×4,
   back arrows, queue↔player, settings→setup/downloads).
5. Works from `file://` with JS disabled (tabs collapse to Artists panel; everything else static).
6. Download states from §8 are consistently rendered everywhere an album appears.
7. `index.html` gallery shows all 10 screens, scaled, clickable.

## 14. Out of scope (do NOT build)

Podcasts, internet radio, bookmarks, jukebox mode, multiple-server switching UI, light
theme, any real JavaScript state/data loading, build tooling, npm packages.
