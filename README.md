# Phaze

> OpenSubsonic-compatible Android music player with offline ambitions.

Phaze is a dark-themed music player for **Subsonic / OpenSubsonic servers**
(Navidrome, Airsonic, Gonic, Funkwhale…). It streams and caches your library,
currently focusing on clean browsing, playback, and a first-class media
notification experience.

- **Design reference:** the HTML mockups in [`mockups/`](mockups/) are the
  authoritative source for the UI. Every screen maps to a mockup.
- **Roadmap:** [`PLAN.md`](PLAN.md) holds the full phased implementation plan.

---

## Features

### Servers & auth
- Add a server: URL normalization (auto `https://`, trailing `/rest` stripped)
  plus a working **ping test** with live status (server type/version).
- OpenSubsonic token auth (`MD5(password + salt)`) with automatic **legacy
  password fallback** for older servers.
- Saved server persisted locally; auth restored automatically on app start.

### Browsing
- **Home** with discovery rails (Recently added / Most played / Recently played
  / Random), a filter grid, a corner-gradient background derived from the theme,
  and real cover art streamed from the server (authenticated Coil).
- **Search** with debounced `search3`, grouped Artist/Album/Song results, and an
  offline-first local-cache fallback.
- **Filter/browse page** shared by every Home tile and Search browse tile —
  albums (grid/list toggle), artists, songs, and playlists; genres/years/moods
  marked "coming soon".
- **Album** detail: hero cover, actions (play/shuffle/star), full track list.
- **Artist** detail: circular avatar, album grid, star toggle.

### Playback
- **Now Playing** screen with live seek bar, play/pause, next/previous,
  shuffle & repeat.
- **Queue** screen backed by the real session queue: drag-to-reorder (with
  auto-scroll), remove, clear, tap-to-play.
- **Mini player** persists above the bottom nav on Home/Search/Library/
  Downloads/Settings/Artist/Album/Filter while something plays.
- **Background playback:** a Media3 `MediaSessionService` — playback continues
  under Battery Saver, the notification has working play/pause + next/previous,
  and the app is recognized as a media app (lock screen / media carousel).
- Streaming goes through the shared authenticated OkHttp client, so `stream`
  requests carry server credentials automatically.

### Settings & appearance
- Server summary + "Test connection" row.
- Playback/downloads/offline/library preferences persisted with DataStore.
- **Accent color picker** — 5 accents re-theme the whole app instantly
  (primary + containers), stored across restarts.

## Not yet implemented
Downloads / offline storage, background library sync workers, playlists detail,
scrobbling wiring, and notification artwork (see [`PLAN.md`](PLAN.md)).

---

## Tech stack

| Concern | Library |
|---|---|
| Language | Kotlin 2.1.0 (K2) |
| UI | Jetpack Compose, Material 3 (BOM 2024.02.00) |
| DI | Hilt 2.56.1 |
| Local DB | Room 2.7.1 (KSP) |
| Network | Retrofit 2.9.0 + OkHttp 4.12 + kotlinx.serialization 1.7.3 |
| Playback | Media3 1.3.0 (ExoPlayer, session, datasource-okhttp) |
| Images | Coil 2.6.0 |
| Preferences | DataStore (Preferences) |
| Background | WorkManager (planned), foreground Service for playback |
| Min / target SDK | 26 / 34 |

---

## Project structure

```
app/src/main/java/com/example/phaze/
├── data/
│   ├── local/          # Room entities, DAOs, database (+ v2 migration)
│   ├── remote/         # Subsonic DTOs, Retrofit API, auth interceptor, converter
│   ├── repository/     # Server, Library, Album, Artist, Search, Settings repos
│   ├── playback/       # PlaybackController (MediaController) + playback state
│   └── mapper/         # DTO ↔ entity ↔ domain mappers
├── playback/           # PlaybackService (MediaSessionService)
├── di/                 # Hilt modules
├── ui/
│   ├── theme/          # M3 theme, accent colors, gradient backgrounds
│   ├── components/     # AlbumArt, MiniPlayer, BottomNav
│   ├── navigation/     # Routes + NavHost
│   └── screens/        # setup, home, search, filter, album, artist, player, queue, downloads, settings, library
└── worker/             # (planned) sync / download workers
```

Companion docs:
- `mockups/` — HTML design mockups (open `mockups/index.html` for the gallery)
- `PLAN.md` — full implementation plan
- `.github/workflows/ci.yml` — CI: compile → tests → release APK + GitHub Release

---

## Getting started

### Prerequisites
- JDK 17
- Android SDK (platform 34; API 26+ devices)

### Build & run

```bash
# Compile check
./gradlew :app:assembleDebug

# Unit tests
./gradlew :app:testDebugUnitTest

# Full build (includes lint)
./gradlew build

# Install on a connected device
./gradlew :app:installDebug
```

The debug APK lands in `app/build/outputs/apk/debug/`.

### Connect a server
1. Open the app → enter your server URL (e.g. `https://music.example.com` or
   `http://192.168.1.10:4533`) and your Subsonic credentials.
2. Tap **Test connection** — you'll see the server type/version on success.
3. Tap **Connect**. Albums stream to the Home rails and covers load over
   authenticated `getCoverArt`.

### CI / releasing
`.github/workflows/ci.yml` runs on every push: compilation check → unit tests →
builds a release APK. Pushing a `v*` tag also **creates a GitHub Release** with
the APKs attached.

- The release APK is **unsigned** unless you add these secrets:
  `ANDROID_KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
  (see the workflow comments for how to generate them).

---

## Testing

Current JVM unit tests live in `app/src/test/…`:
- `SubsonicApiTest` — response envelope decoding (Navidrome-shaped JSON),
  numeric-ID coercion, error mapping.
- `SubsonicAuthTest` — token/salt generation, auth configs.
- `SubsonicMappersTest` — DTO → entity mapping preserving local state.
- `ServerRepositoryTest` — server URL normalization.

---

## License / note

Private project — see `OVERVIEW.md` / `PLAN.md` for background. The mockups use
real album placeholders (gradients) purely as design examples; no artwork is
bundled.
