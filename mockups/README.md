# Phaze — UI Mockups

Static, self-contained HTML/CSS mockups for a **Subsonic-compatible Android music player**
(Navidrome / Airsonic / Gonic …) with **offline download support**. Material Design 3,
dark theme. Pure HTML + CSS + a few lines of vanilla JS — no frameworks, no CDNs, no images,
work offline from `file://`.

## Viewing

```sh
# Gallery (all 10 screens at a glance, click any to open it)
xdg-open mockups/index.html

# …or serve the folder and open http://localhost:8000
cd mockups && python3 -m http.server 8000
```

Any static file server works. Every screen also opens standalone
(e.g. `mockups/screens/home.html`).

## File map

```
mockups/
├── index.html            gallery: all screens side by side
├── README.md
├── assets/
│   ├── app.css           shared design system (M3 dark tokens + components)
│   └── app.js            tabs, toggles, star, mini-player (progressive enhancement)
└── screens/
    ├── setup.html        server connection
    ├── home.html         discovery rails + offline banner
    ├── library.html      artists / albums / songs / playlists tabs
    ├── artist.html       artist detail
    ├── album.html        album detail + track list
    ├── player.html       now playing
    ├── queue.html        up next
    ├── downloads.html    storage + downloaded items + offline switches
    ├── search.html       search bar + recent searches + 3-col category grid (albums / artists / genres / songs / playlists / years / moods)
    └── settings.html     server / playback / downloads / offline / appearance
```

## Screen → Subsonic API mapping

| Screen | API |
|---|---|
| setup | `ping`, `getLicense`, authentication (salt + token) |
| home | `getAlbumList2` — `newest` / `frequent` / `recent` / `random` |
| library | `getArtists`, `getAlbumList`, `getSongs`, `getPlaylists` |
| artist | `getArtist`, `getAlbum` |
| album | `getAlbum`, `star`, `scrobble` |
| player | `stream` (bitrate/format), `scrobble`, `star` |
| queue | client-side playback queue |
| downloads | `download`, `getDownloadedSongs` (client cache) |
| search | `search3` |
| settings | client preferences |

## Design notes

- Tokens live in `:root` in `assets/app.css` — these map 1:1 to a future
  Jetpack Compose **Material 3** theme (colorScheme, typography, shapes).
- Album art is placeholder: CSS gradients + initials, with a green check badge for
  downloaded items and a progress bar for in-flight downloads.
- All icons are inline SVG (Material Symbols paths) — swap with your icon set later.
- The mockups reproduce Subsonic concepts: download-state badges, offline mode,
  storage limits, quality chips, scrobbling.
