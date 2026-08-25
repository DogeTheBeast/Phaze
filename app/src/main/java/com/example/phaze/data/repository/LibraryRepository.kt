package com.example.phaze.data.repository

import android.util.Log
import com.example.phaze.data.local.dao.AlbumDao
import com.example.phaze.data.local.dao.ArtistDao
import com.example.phaze.data.local.dao.PlaylistDao
import com.example.phaze.data.local.dao.SongDao
import com.example.phaze.data.local.entity.AlbumEntity
import com.example.phaze.data.mapper.*
import com.example.phaze.data.model.Album
import com.example.phaze.data.model.Artist
import com.example.phaze.data.model.DownloadState
import com.example.phaze.data.model.Playlist
import com.example.phaze.data.model.Song
import com.example.phaze.data.remote.SubsonicApi
import com.example.phaze.data.remote.SubsonicApiProvider
import com.example.phaze.data.remote.dto.AlbumID3
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Library sync + cache for the home rails (PLAN.md §4/§5, Phase 2).
 *
 * `getAlbumList2` is fetched for the four rail types and upserted into Room;
 * the UI observes the cached rows so it renders instantly and stays correct
 * offline. Rail-specific ordering info is persisted on the row:
 *   newest  → `created`   (drives "Recently added")
 *   frequent→ `playCount` (drives "Most played")
 *   recent  → `lastPlayed`(drives "Recently played"; synthesized since the API
 *             returns only an ordering, not a timestamp)
 *   random  → query uses ORDER BY RANDOM()
 */
@Singleton
class LibraryRepository @Inject constructor(
    private val apiProvider: SubsonicApiProvider,
    private val serverRepository: ServerRepository,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
) {

    companion object {
        const val RAIL_LIMIT = 20
        private const val TAG = "LibraryRepository"
    }

    // ---- Observation (cached in Room) ----

    fun observeRecentlyAdded(limit: Int = RAIL_LIMIT): Flow<List<AlbumEntity>> =
        albumDao.observeRecentlyAdded(limit)

    fun observeMostPlayed(limit: Int = RAIL_LIMIT): Flow<List<AlbumEntity>> =
        albumDao.observeMostPlayed(limit)

    fun observeRecentlyPlayed(limit: Int = RAIL_LIMIT): Flow<List<AlbumEntity>> =
        albumDao.observeRecentlyPlayed(limit)

    fun observeRandom(limit: Int = RAIL_LIMIT): Flow<List<AlbumEntity>> =
        albumDao.observeRandom(limit)

    // ---- Filter page observation (domain models with cover URLs) ----

    /** Albums for a filter key: added/recent/frequent/least/random/starred/downloaded/all. */
    fun observeAlbums(type: String, limit: Int = 100): Flow<List<Album>> {
        val entities: Flow<List<AlbumEntity>> = when (type) {
            "added" -> albumDao.observeRecentlyAdded(limit)
            "recent" -> albumDao.observeRecentlyPlayed(limit)
            "frequent" -> albumDao.observeMostPlayed(limit)
            "least" -> albumDao.observeLeastPlayed(limit)
            "random" -> albumDao.observeRandom(limit)
            "starred", "newly-starred" -> albumDao.observeStarred(limit)
            "downloaded" -> albumDao.observeDownloaded()
            else -> albumDao.observeAll()
        }
        return combine(serverRepository.observeActiveServer(), entities) { server, list ->
            list.map { it.toModel(server?.url) }
        }
    }

    fun observeArtists(): Flow<List<Artist>> =
        combine(serverRepository.observeActiveServer(), artistDao.observeAll()) { server, list ->
            list.map { it.toModel(server?.url) }
        }

    fun observeSongs(): Flow<List<Song>> =
        combine(serverRepository.observeActiveServer(), songDao.observeAll()) { server, list ->
            list.map { it.toModel(server?.url) }
        }

    fun observePlaylists(): Flow<List<Playlist>> =
        playlistDao.observeAll().map { list -> list.map { it.toModel() } }

    /**
     * A single random snapshot for the Random filter page. Captured once per
     * visit so navigating back doesn't re-shuffle the albums.
     */
    suspend fun getRandomAlbums(limit: Int = 100): List<Album> {
        val server = serverRepository.getActiveServer()
        return albumDao.getRandom(limit).map { it.toModel(server?.url) }
    }

    // ---- Sync ----

    /**
     * Fetches all four home rails from the active server and upserts them.
     * No-op failure (Result.failure) when no server is configured.
     */
    suspend fun syncHomeRails(limit: Int = RAIL_LIMIT): Result<Unit> {
        val server = serverRepository.getActiveServer()
            ?: return Result.failure(IllegalStateException("No server configured"))
        Log.d(TAG, "syncHomeRails: fetching rails for '${server.url}' (limit=$limit)")

        return try {
            val api = apiProvider.api(server.url)
            val entities = coroutineScope {
                val newest = async { fetchRail(api, "newest", limit, "recently added") }
                val frequent = async { fetchRail(api, "frequent", limit, "most played") }
                val recent = async { fetchRail(api, "recent", limit, "recently played") }
                val random = async { fetchRail(api, "random", limit, "random") }
                mergeRails(newest.await(), frequent.await(), recent.await(), random.await())
            }
            if (entities.isNotEmpty()) {
                albumDao.upsertAll(entities)
                Log.d(TAG, "syncHomeRails: upserted ${entities.size} albums into Room")
            } else {
                Log.d(TAG, "syncHomeRails: server returned no albums")
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.e(TAG, "syncHomeRails: failed - ${e::class.simpleName}: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Refreshes the data backing a filter page. Rail/album types reuse
     * [syncHomeRails]; artists/songs/playlists/starred fetch their own endpoint.
     */
    suspend fun syncFilter(type: String): Result<Unit> {
        val server = serverRepository.getActiveServer()
            ?: return Result.failure(IllegalStateException("No server configured"))
        return try {
            val result: Result<Unit> = when (type) {
                "artists" -> { fetchArtists(); Result.success(Unit) }
                "songs" -> { fetchSongs(); Result.success(Unit) }
                "playlists" -> { fetchPlaylists(); Result.success(Unit) }
                "starred", "newly-starred" -> { fetchStarred(); Result.success(Unit) }
                "downloaded" -> Result.success(Unit)
                else -> syncHomeRails()
            }
            result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.e(TAG, "syncFilter('$type') failed - ${e::class.simpleName}: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun fetchArtists() {
        val api = apiProvider.api(serverRepository.getActiveServer()?.url.orEmpty())
        val artists = api.getArtists().data?.artists?.index.orEmpty().flatMap { it.artist }
        artistDao.upsertAll(artists.map { it.toEntity() })
        Log.d(TAG, "fetchArtists: upserted ${artists.size} artists")
    }

    private suspend fun fetchSongs() {
        val api = apiProvider.api(serverRepository.getActiveServer()?.url.orEmpty())
        val songs = api.getSongs(size = 500).data?.songs?.song.orEmpty()
        songDao.upsertAll(songs.map { it.toEntity(albumId = it.albumId, artistId = it.artistId) })
        Log.d(TAG, "fetchSongs: upserted ${songs.size} songs")
    }

    private suspend fun fetchPlaylists() {
        val api = apiProvider.api(serverRepository.getActiveServer()?.url.orEmpty())
        val playlists = api.getPlaylists().data?.playlists?.playlist.orEmpty()
        playlistDao.upsertAll(playlists.map { it.toEntity() })
        Log.d(TAG, "fetchPlaylists: upserted ${playlists.size} playlists")
    }

    private suspend fun fetchStarred() {
        val api = apiProvider.api(serverRepository.getActiveServer()?.url.orEmpty())
        val albums = api.getStarred2().data?.starred?.album.orEmpty()
        val existing = albums.mapNotNull { albumDao.getById(it.id) }.associateBy { it.id }
        albumDao.upsertAll(
            albums.map { album ->
                album.toEntity().copy(
                    starred = true,
                    downloadState = existing[album.id]?.downloadState ?: DownloadState.NONE,
                )
            }
        )
        Log.d(TAG, "fetchStarred: upserted ${albums.size} starred albums")
    }

    private suspend fun fetchRail(api: SubsonicApi, type: String, limit: Int, label: String): List<AlbumID3> {
        Log.d(TAG, "fetchRail: requesting getAlbumList2 type='$type' label='$label' size=$limit")
        return try {
            val albums = api.getAlbumList2(type = type, size = limit).data?.albumsList?.albums?: emptyList()
            Log.d(TAG, "fetchRail: type='$type' returned ${albums.size} albums (first=${albums.firstOrNull()?.name})")
            albums
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.w(TAG, "fetchRail: type='$type' failed - ${e::class.simpleName}: ${e.message}")
            emptyList()
        }
    }

    /**
     * Collects the four rails into one deduplicated set, keeping the richest
     * info per album: the newest created date, the highest play count, and any
     * synthesized last-played timestamp.
     */
    private fun mergeRails(
        newest: List<AlbumID3>,
        frequent: List<AlbumID3>,
        recent: List<AlbumID3>,
        random: List<AlbumID3>,
    ): List<AlbumEntity> {
        val now = System.currentTimeMillis()
        val merged = LinkedHashMap<String, AlbumEntity>()

        fun put(entity: AlbumEntity) {
            val prev = merged[entity.id]
            merged[entity.id] = if (prev == null) {
                entity
            } else {
                prev.copy(
                    created = maxOf(prev.created, entity.created),
                    playCount = maxOf(prev.playCount, entity.playCount),
                    lastPlayed = entity.lastPlayed ?: prev.lastPlayed,
                )
            }
        }

        newest.forEach { put(it.toEntity()) }
        frequent.forEach { put(it.toEntity()) }
        // getAlbumList2 type=recent returns albums in recency order without a
        // timestamp, so store a descending synthetic value to keep that order.
        recent.forEachIndexed { index, album -> put(album.toEntity().copy(lastPlayed = now - index * 1_000L)) }
        random.forEach { put(it.toEntity()) }

        val result = merged.values.toList()
        Log.d(TAG, "mergeRails: deduped ${newest.size + frequent.size + recent.size + random.size} entries into ${result.size} distinct albums")
        return result
    }
}
