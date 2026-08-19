# Phaze2 — Implementation Plan

> OpenSubsonic-compatible Android music player with offline support.
> Written in Kotlin with Jetpack Compose, Material 3, and Media3.
> The mockups in `mockups/` are the authoritative design reference.

---

## 1. Goals & Constraints

| Goal | Detail |
|------|--------|
| Server support | OpenSubsonic / Subsonic REST API (Navidrome, Airsonic, Gonic, etc.) |
| Offline-first | Downloads, cache, offline mode, storage management |
| UI | Jetpack Compose + Material 3, dark theme (see `theme.yaml`), but also customizable by the user, no hardcoded color values |
| Playback | Background audio, gapless, queue |
| Min SDK | API 26 (Android 8.0) — covers 97%+ of active devices |
| Target SDK | Latest stable |

**Out of scope for v1:** podcasts, internet radio, jukebox, multi-user, Wear OS, TV.

---

## 2. Architecture

```
┌─────────────────────────────────────────────┐
│  UI Layer (Compose + ViewModel + StateFlow)  │
│  Screens, Navigation, M3 Theme              │
├─────────────────────────────────────────────┤
│  Domain Layer (Use Cases / Interactors)      │
│  Business logic, offline rules, queue ops   │
├─────────────────────────────────────────────┤
│  Data Layer (Repository + Room + Retrofit)    │
│  Remote API, local DB, download manager, FS   │
└─────────────────────────────────────────────┘
```

- **UDF (Unidirectional Data Flow):** UI emits events → ViewModel → UseCase → Repository → StateFlow → UI recomposes.
- **DI:** Hilt for everything injectable.
- **Coroutines + Flow:** all async work; `Dispatchers.IO` for DB/network.

---

## 3. Tech Stack

| Concern | Library |
|---------|---------|
| UI | Jetpack Compose (latest), Material 3 |
| Navigation | Navigation Compose |
| Networking | Retrofit 2 + OkHttp + kotlinx.serialization |
| Local DB | Room (KSP) |
| Preferences | DataStore (typed + proto optional) |
| Media playback | Media3 ExoPlayer + MediaSession + AudioFocus |
| Images | Coil (loads `getCoverArt` with auth token) |
| Downloads | WorkManager + OkHttp streaming write |
| Background | WorkManager (sync, downloads), ForegroundService (playback) |
| Crypto | Android Keystore (server password optional) |

---

## 4. API Layer — OpenSubsonic REST

Base endpoint: `https://server/rest/`
Authentication: username + token (MD5(password + salt)) + salt. Also support legacy `password=` raw for older servers.

**Core endpoints to implement:**

| Endpoint | Purpose |
|----------|---------|
| `ping` / `getLicense` | Connectivity & auth validation |
| `getArtists` | Library → Artists tab |
| `getAlbumList2` | Home rails: `newest`, `frequent`, `recent`, `random` |
| `getAlbum` | Album detail + track list |
| `getArtist` | Artist detail + albums |
| `getSongs` | Library → Songs tab (paginated) |
| `getPlaylists` / `getPlaylist` | Library → Playlists |
| `search3` | Search screen |
| `stream` | Playback URL (with `maxBitRate`, `format`, `id`) |
| `download` | Download file (same as stream but file attachment) |
| `star` / `unstar` | Star/unstar albums, songs, artists |
| `scrobble` | Report playback to server |
| `getCoverArt` | Album/artist images (size param) |
| `getStarred2` | Starred items (for filters & auto-download) |

**OpenSubsonic extensions (nice-to-have):**
- `getAlbumList2` `type=starred` for starred album rail
- Bookmark endpoints for resume playback position

Retrofit service returns sealed classes; wrap network errors into a `Result<T>` domain type.

---

## 5. Data Layer — Room Schema

### Entities

```kotlin
@Entity
data class ServerEntity(
    @PrimaryKey val id: Long = 0,
    val url: String,
    val username: String,
    val token: String?,        // nullable if legacy auth
    val salt: String?,
    val useLegacyAuth: Boolean,
    val serverType: String?   // "Navidrome", etc.
)

@Entity
data class ArtistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val albumCount: Int,
    val coverArt: String?
)

@Entity
data class AlbumEntity(
    @PrimaryKey val id: String,
    val name: String,
    val artistId: String,
    val artistName: String,
    val year: Int?,
    val songCount: Int,
    val duration: Int,
    val coverArt: String?,
    val created: Long,
    val starred: Boolean,
    val downloadState: DownloadState   // NONE, IN_PROGRESS, DOWNLOADED
)

@Entity
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val albumId: String,
    val artistId: String,
    val artistName: String,
    val track: Int,
    val duration: Int,
    val bitrate: Int?,
    val format: String?,          // "mp3", "flac", ...
    val contentType: String?,
    val size: Long,               // bytes
    val coverArt: String?,
    val starred: Boolean,
    val downloadState: DownloadState,
    val localPath: String?        // filesystem path if downloaded
)

@Entity
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val songCount: Int,
    val duration: Int,
    val created: Long,
    val public: Boolean
)

@Entity
data class PlaylistSongCrossRef(
    val playlistId: String,
    val songId: String,
    val position: Int
)

@Entity
data class DownloadJobEntity(
    @PrimaryKey val songId: String,
    val status: DownloadStatus,   // PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val startedAt: Long?,
    val completedAt: Long?,
    val failureReason: String?
)
```

### DAOs
- `ArtistDao`, `AlbumDao`, `SongDao`, `PlaylistDao`, `DownloadDao`
- `AlbumDao.getRecentlyAdded()`, `getMostPlayed()`, `getRecentlyPlayed()`, `getRandom()`
- `SongDao.getSongsForAlbum(albumId)`, `search(query)`
- `DownloadDao.getInProgress()`, `getDownloadedSongs()`, `getDownloadedAlbums()`

---

## 6. UI Layer — Compose Screen Mapping

Every mockup screen becomes a `@Composable` destination in the Navigation graph.

| Screen (mockup) | Composable | Notes |
|---|---|---|
| **Setup** | `ServerSetupScreen` | URL validation, credential store, ping test, saved-server list |
| **Home** | `HomeScreen` | `LazyColumn` with `LazyRow` rails, hero logo, filter grid, offline chip |
| **Library** | `LibraryScreen` | M3 secondary tabs (Artists/Albums/Songs/Playlists). Artists = `LazyColumn` with sticky-ish alphabet header? Use `LazyColumn` index + fast-scroll. |
| **Artist** | `ArtistDetailScreen` | Header circle + action row (Play/Shuffle/Star), album grid, similar-artist chips |
| **Album** | `AlbumDetailScreen` | Hero cover, action FABs, track list with per-track download icons |
| **Player** | `NowPlayingScreen` | Full-screen sheet; cover + seek + transport + bottom chips |
| **Queue** | `QueueScreen` | Up-next list, current-track highlight, save-as-playlist, clear |
| **Downloads** | `DownloadsScreen` | Storage bar (Compose linear indicator), in-progress + downloaded lists, offline toggle |
| **Search** | `SearchScreen` | Search field + `LazyColumn` grouped results; debounced query |
| **Settings** | `SettingsScreen` | Grouped preferences list; switches use `Switch` M3 |

**Shared components (extract as @Composable):**
- `MiniPlayer` — persistent bottom bar above nav bar (streams `PlayerViewModel.state`).
- `BottomNav` — M3 NavigationBar, 4 items: Home, Search, Downloads, Settings.
- `GradientArt` — custom `drawBehind` or `Box` with gradient + initials (no images needed; server art via Coil).
- `SeekBar` — custom slider using `Slider` M3 or Canvas for buffered/fill/thumb.

**Theme:**
- Create `MaterialTheme` from the `theme.yaml` tokens (see §9).
- `ColorScheme` with `primary = base0D`, `secondary = base0E`, `tertiary = base0C`, `error = base08`.
- `Typography` from M3 type scale (no external fonts).
- Color theme customizable by the user, no hardcoded values

---

## 7. Media & Playback Architecture

Use **Media3 ExoPlayer** with a **MediaSessionService** so playback continues in background and integrates with system media controls (lock screen, notification, Bluetooth, Android Auto via MediaBrowser).

### Components

1. **ExoPlayer instance** managed in a `MediaSessionService` (`PlaybackService`).
2. **Queue management:** maintain a `List<MediaItem>` in the service backed by a domain `QueueManager`. Queue survives config changes via service binding.
3. **Audio focus:** ExoPlayer handles it automatically when configured.
4. **Notification:** Media3 `MediaNotificationProvider` with album art (loaded via Coil into a `Bitmap` for the session).
5. **Scrobbling:** observe `PlaybackState` transitions to `STATE_PLAYING`; after ~4s or halfway, call `scrobble` endpoint.

### Playback sources
- **Streaming:** `stream?id=` URL with auth params appended. ExoPlayer handles buffering.
- **Downloaded:** if `SongEntity.localPath` exists, build `MediaItem` from local `File` URI; else fall back to stream.
- **Transcoding:** append `maxBitRate` and `format` per Settings (Wi-Fi vs mobile, per-user quality prefs).

### SeekBar synchronization
- Expose `Player.position` / `duration` / `bufferedPosition` via `StateFlow` from a `PlayerViewModel` that observes the service via `MediaController` (Media3).
- Compose `Slider` bound to `position` + `duration`.

---

## 8. Offline / Download System

### State machine

```
SongEntity.downloadState
  NONE ──[enqueue]──► PENDING
  PENDING ──[start]──► IN_PROGRESS
  IN_PROGRESS ──[finish]──► DOWNLOADED
  IN_PROGRESS ──[cancel/fail]──► NONE
```

### Download manager
- **Enqueue:** insert `DownloadJobEntity(PENDING)`, then schedule a WorkManager `OneTimeWorkRequest`.
- **Worker:** `DownloadWorker` runs on `Dispatchers.IO`, streams via OkHttp `ResponseBody`, writes to `context.filesDir/downloads/<songId>.<ext>` with a temp suffix, then renames on completion.
- **Progress:** emit progress % via `setProgress()` in WorkManager; observe in UI via `WorkInfo` → `DownloadViewModel`.
- **Cancellation:** tapping the cancel button on the in-progress row calls `WorkManager.cancelWorkById()` and marks job CANCELLED.
- **Delete:** remove file from disk, set `SongEntity.downloadState = NONE`, `localPath = null`.

### Storage accounting
- Sum `SongEntity.size` where `downloadState == DOWNLOADED`.
- Compare against `maxStorageBytes` (from Settings, default 8 GB).
- Show a `LinearProgressIndicator` on Downloads screen.

### Auto-download rules (background)
- On Wi-Fi connect: if `autoDownloadStarred` is ON, enqueue downloads for all starred songs where `downloadState == NONE`.
- On star event: if `autoDownloadStarred` is ON and Wi-Fi is active, enqueue immediately.

### Offline mode
- A global boolean in DataStore: `offlineMode`.
- When ON, all library queries filter to `WHERE downloadState = DOWNLOADED`.
- Home rails also filter; if empty, show empty-state "Download music for offline".

---

## 9. Phased Implementation Roadmap

### Phase 0 — Scaffold (week 1)
- [ ] Kotlin project with Compose, Navigation, Hilt, Room, Retrofit, Media3, Coil, WorkManager dependencies.
- [ ] Git repo with `mockups/` preserved as design reference.
- [ ] M3 `ColorScheme` + `Typography` extracted from `mockups/theme.yaml`.
- [ ] Navigation graph skeleton: all 10 destinations + bottom nav + mini-player slot.
- [ ] `PlaybackService` stub (bound, no-op).

### Phase 1 — Server + Auth (week 1–2)
- [ ] Retrofit service + auth interceptor (salt/token generator, legacy fallback).
- [ ] `ServerSetupScreen` (UI from mockup §9.1), ping validation, DataStore persistence.
- [ ] `SettingsScreen`: server row, test connection, saved-server list.
- [ ] Room `ServerEntity`, `ServerDao`.

### Phase 2 — Library Sync + Offline Foundation (week 2–3)
- [ ] Sync workers: `getArtists`, `getAlbumList2` (newest/frequent/recent/random), `getAlbum`, `getPlaylists`.
- [ ] Room schema: `Artist`, `Album`, `Song`, `Playlist` entities + DAOs + relations.
- [ ] Repository pattern: `LibraryRepository` (remote → DB → Flow).
- [ ] Download worker scaffolding + file storage path logic.
- [ ] `DownloadsScreen`: storage bar, in-progress list, downloaded lists, delete actions.

### Phase 3 — Playback + Queue (week 3–4)
- [ ] ExoPlayer integration in `PlaybackService`.
- [ ] `MediaSession` + notification + lock-screen controls.
- [ ] `NowPlayingScreen` (cover, seek, transport, quality chip).
- [ ] `QueueScreen` + queue management (add/remove/reorder).
- [ ] `MiniPlayer` persistent above bottom nav (bound to service state).

### Phase 4 — Home + Discovery (week 4–5)
- [ ] `HomeScreen`: hero logo, filter grid, rails (recently added / most played / recently played / random).
- [ ] Rail cards: `LazyRow` of gradient-art covers with Coil fallback.
- [ ] Offline banner chip in `HomeScreen` when `offlineMode` is ON.

### Phase 5 — Search + Detail Screens (week 5–6)
- [ ] `SearchScreen` with debounced `search3` + grouped results (Artists / Albums / Songs).
- [ ] `ArtistDetailScreen`, `AlbumDetailScreen` (track list with per-track download state).
- [ ] Star/unstar actions everywhere (artist, album, song row).

### Phase 6 — Polish + Release Prep (week 6–7)
- [ ] Gapless playback toggle (ExoPlayer `setHandleAudioBecomingNoisy`, gapless config).
- [ ] Streaming quality per network (DataStore prefs, Retrofit request params).
- [ ] Scrobbling (playback event listener → `scrobble` call).
- [ ] Edge-to-edge, insets handling, predictive back gesture (Android 13+).
- [ ] Baseline profiles, release build, signing.

---

## 10. Testing Strategy

| Layer | Approach |
|---|---|
| **ViewModel** | Turbine + `TestDispatcher` for StateFlow assertions |
| **Repository** | Fake DAO + MockWebServer for Retrofit; assert DB state after sync |
| **UseCase / Sync** | End-to-end with `TestWorkManager`; validate `WorkInfo` output |
| **Playback** | Media3 `TestPlayer` + `Robolectric` for service lifecycle |
| **UI (screens)** | Compose `createComposeRule` + semantics assertions; screenshot diffs optional |
| **Integration** | Install against a local Navidrome Docker container; run full smoke test |

---

## 11. Project Structure (suggested)

```
app/src/main/java/com/example/phaze2/
├── Phaze2Application.kt
├── di/
│   ├── AppModule.kt          (singletons: DB, Retrofit, ExoPlayer, DataStore)
│   ├── RepositoryModule.kt
│   └── ViewModelModule.kt
├── data/
│   ├── local/                (Room entities, DAOs, Database)
│   ├── remote/               (Retrofit service, DTOs, auth interceptor)
│   ├── repository/           (LibraryRepository, PlaybackRepository, DownloadRepository, SettingsRepository)
│   └── model/
│       ├── Server.kt
│       ├── Artist.kt, Album.kt, Song.kt, Playlist.kt
│       ├── DownloadState.kt, DownloadStatus.kt
│       └── PlaybackState.kt, QueueState.kt
├── domain/
│   ├── usecase/              (SyncLibraryUseCase, EnqueueDownloadUseCase, ScrobbleUseCase, etc.)
│   └── manager/              (QueueManager, DownloadManager, OfflineModeManager)
├── playback/
│   ├── PlaybackService.kt    (Media3 MediaSessionService)
│   ├── PlayerController.kt   (MediaController wrapper)
│   └── notification/
├── ui/
│   ├── theme/                (Color.kt, Type.kt, Theme.kt derived from theme.yaml)
│   ├── components/           (MiniPlayer.kt, BottomNav.kt, GradientArt.kt, SeekBar.kt, DownloadBadge.kt)
│   ├── screens/
│   │   ├── setup/
│   │   ├── home/
│   │   ├── library/
│   │   ├── artist/
│   │   ├── album/
│   │   ├── player/
│   │   ├── queue/
│   │   ├── downloads/
│   │   ├── search/
│   │   └── settings/
│   └── navigation/
│       └── Phaze2NavHost.kt
└── worker/
    ├── LibrarySyncWorker.kt
    ├── DownloadWorker.kt
    └── AutoDownloadWorker.kt
```

---

## 12. Reference Files

| File | Purpose |
|---|---|
| `mockups/index.html` | Visual spec — every screen at a glance |
| `mockups/screens/*.html` | Individual click-through screens |
| `mockups/assets/app.css` | Token values (colors, typography, spacing) → map to `ColorScheme` & `Typography` |
| `mockups/theme.yaml` | Base16 palette → seed colors for M3 dynamic/tonal mapping |
| `mockups/README.md` | Subsonic API mapping per screen |

---

## 13. Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Server auth variations (salt vs legacy) | Test against Navidrome, Airsonic, and Ampache; support both auth modes with auto-fallback. |
| Large library sync → ANR | Paginated sync; incremental updates via `getAlbumList2` `size=50`; use `WorkManager` with constraints. |
| Background playback killed | MediaSessionService + foreground notification + `startForeground` in `onStartCommand`. |
| Storage exhaustion | Pre-enqueue check: `currentUsed + song.size > limit` → show toast, skip. |
| DRM / transcoded streams | ExoPlayer handles most codecs; fail gracefully with "transcode to MP3" fallback toggle. |

---

## 14. Next Step

Begin **Phase 0** — scaffold the project, wire dependencies, and create the `ColorScheme` + `Typography` from `theme.yaml`. Once the empty navigation shell runs on-device with the correct dark theme, move to Phase 1 (server connection + auth).
