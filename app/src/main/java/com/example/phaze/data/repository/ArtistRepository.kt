package com.example.phaze.data.repository

import android.util.Log
import com.example.phaze.data.local.dao.AlbumDao
import com.example.phaze.data.local.dao.ArtistDao
import com.example.phaze.data.mapper.toEntity
import com.example.phaze.data.mapper.toModel
import com.example.phaze.data.model.ArtistDetail
import com.example.phaze.data.model.DownloadState
import com.example.phaze.data.remote.CoverArtUrl
import com.example.phaze.data.remote.SubsonicApiProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Backend for the artist detail screen (PLAN.md §5/§8, mockup artist.html).
 *
 * `load` fetches `getArtist` (header + album list) and caches it in Room;
 * `observe` surfaces the cached artist + albums reactively. `star/unstar` is
 * handled server-side and mirrored locally.
 */
@Singleton
class ArtistRepository @Inject constructor(
    private val apiProvider: SubsonicApiProvider,
    private val serverRepository: ServerRepository,
    private val artistDao: ArtistDao,
    private val albumDao: AlbumDao,
) {

    companion object {
        private const val TAG = "ArtistRepository"
    }

    fun observe(id: String): Flow<ArtistDetail> = combine(
        serverRepository.observeActiveServer(),
        artistDao.observeById(id),
        albumDao.observeByArtist(id),
    ) { server, artist, albums ->
        ArtistDetail(
            id = id,
            name = artist?.name ?: "",
            coverArtUrl = artist?.coverArt?.let { server?.url?.let { s -> CoverArtUrl.of(s, it, 512) } },
            albumCount = artist?.albumCount ?: albums.size,
            songCount = albums.sumOf { it.songCount },
            starred = artist?.starred ?: false,
            albums = albums.map { it.toModel(server?.url) },
        )
    }

    /** Fetches artist detail + albums from the server and caches into Room. */
    suspend fun load(id: String): Result<Unit> {
        val server = serverRepository.getActiveServer()
            ?: return Result.failure(IllegalStateException("No server configured"))
        return try {
            val detail = apiProvider.api(server.url).getArtist(id).data?.artist
                ?: return Result.failure(IllegalStateException("Artist not found"))

            // Preserve local-only state (star, download) across refreshes.
            val existingArtist = artistDao.getById(id)?.starred ?: false
            artistDao.upsert(detail.toEntity().copy(starred = existingArtist))

            val existingAlbums = detail.album.mapNotNull { albumDao.getById(it.id) }.associateBy { it.id }
            albumDao.upsertAll(
                detail.album.map { album ->
                    val prev = existingAlbums[album.id]
                    album.toEntity().copy(
                        starred = prev?.starred ?: (album.starred != null),
                        downloadState = prev?.downloadState ?: DownloadState.NONE,
                    )
                }
            )
            Log.d(TAG, "load('$id'): saved artist '${detail.name}' + ${detail.album.size} albums")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.e(TAG, "load('$id') failed - ${e::class.simpleName}: ${e.message}", e)
            Result.failure(e)
        }
    }

    /** Stars/unstars the artist server-side and mirrors it in the local cache. */
    suspend fun toggleStar(id: String): Result<Boolean> {
        val server = serverRepository.getActiveServer()
            ?: return Result.failure(IllegalStateException("No server configured"))
        val target = !(artistDao.getById(id)?.starred ?: false)
        return try {
            val api = apiProvider.api(server.url)
            if (target) api.star(artistId = id) else api.unstar(artistId = id)
            artistDao.setStarred(id, target)
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
