package com.example.phaze2.data.repository

import android.util.Log
import com.example.phaze2.data.local.dao.AlbumDao
import com.example.phaze2.data.local.entity.AlbumEntity
import com.example.phaze2.data.mapper.toEntity
import com.example.phaze2.data.remote.SubsonicApi
import com.example.phaze2.data.remote.SubsonicApiProvider
import com.example.phaze2.data.remote.dto.AlbumID3
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow

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
