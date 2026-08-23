package com.example.phaze.data.repository

import android.util.Log
import com.example.phaze.data.local.dao.AlbumDao
import com.example.phaze.data.local.dao.SongDao
import com.example.phaze.data.mapper.toEntity
import com.example.phaze.data.mapper.toModel
import com.example.phaze.data.model.AlbumDetail
import com.example.phaze.data.remote.CoverArtUrl
import com.example.phaze.data.remote.SubsonicApiProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Backend for the album detail screen (PLAN.md §5/§8, mockup album.html).
 *
 * `load` fetches `getAlbum` (header + tracks) from the server and caches it in
 * Room; `observe` surfaces the cached album + its track list reactively (so it
 * renders instantly and stays correct offline). `star/unstar` is also handled.
 */
@Singleton
class AlbumRepository @Inject constructor(
    private val apiProvider: SubsonicApiProvider,
    private val serverRepository: ServerRepository,
    private val albumDao: AlbumDao,
    private val songDao: SongDao,
) {

    companion object {
        private const val TAG = "AlbumRepository"
    }

    /** Reactive album header + tracks, with cover art resolved against the active server. */
    fun observe(id: String): Flow<AlbumDetail> = combine(
        serverRepository.observeActiveServer(),
        albumDao.observeById(id),
        songDao.observeByAlbum(id),
    ) { server, album, songs ->
        AlbumDetail(
            id = id,
            name = album?.name ?: "",
            artistName = album?.artistName ?: "",
            artistId = album?.artistId ?: "",
            year = album?.year,
            songCount = album?.songCount ?: songs.size,
            duration = album?.duration ?: songs.sumOf { it.duration },
            starred = album?.starred ?: false,
            coverArtUrl = album?.coverArt?.let { server?.url?.let { s -> CoverArtUrl.of(s, it, CoverArtUrl.FULL) } },
            songs = songs.map { it.toModel(server?.url) },
        )
    }

    /** Fetches album detail + tracks from the server and upserts into Room. */
    suspend fun load(id: String): Result<Unit> {
        val server = serverRepository.getActiveServer()
            ?: return Result.failure(IllegalStateException("No server configured"))
        return try {
            val detail = apiProvider.api(server.url).getAlbum(id).data?.album
                ?: return Result.failure(IllegalStateException("Album not found"))
            val existing = songDao.getSongsForAlbum(id).associateBy { it.id }
            albumDao.upsert(detail.toEntity())
            songDao.upsertAll(
                detail.song.map { track ->
                    track.toEntity(albumId = id, artistId = detail.artistId, existing = existing[track.id])
                }
            )
            Log.d(TAG, "load('$id'): saved album '${detail.name}' + ${detail.song.size} tracks")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.e(TAG, "load('$id') failed - ${e::class.simpleName}: ${e.message}", e)
            Result.failure(e)
        }
    }

    /** Stars/unstars the album server-side and mirrors it in the local cache. */
    suspend fun toggleStar(id: String): Result<Boolean> {
        val server = serverRepository.getActiveServer()
            ?: return Result.failure(IllegalStateException("No server configured"))
        val target = !(albumDao.getById(id)?.starred ?: false)
        return try {
            val api = apiProvider.api(server.url)
            if (target) api.star(albumId = id) else api.unstar(albumId = id)
            albumDao.setStarred(id, target)
            Log.d(TAG, "toggleStar('$id') -> $target")
            Result.success(target)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.e(TAG, "toggleStar('$id') failed - ${e::class.simpleName}: ${e.message}", e)
            Result.failure(e)
        }
    }
}
