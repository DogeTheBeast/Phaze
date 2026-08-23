package com.example.phaze.data.repository

import android.util.Log
import com.example.phaze.data.local.dao.AlbumDao
import com.example.phaze.data.local.dao.ArtistDao
import com.example.phaze.data.local.dao.SongDao
import com.example.phaze.data.mapper.toModel
import com.example.phaze.data.mapper.toSongModel
import com.example.phaze.data.model.Album
import com.example.phaze.data.model.Artist
import com.example.phaze.data.model.SearchResults
import com.example.phaze.data.model.Song
import com.example.phaze.data.remote.SubsonicApiProvider
import com.example.phaze.data.remote.dto.SearchResult3
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Backend for search (PLAN.md §5, §8, mockup search.html).
 *
 * Offline-first strategy:
 *  1. Query the **local Room cache** for artists/albums/songs matching the term
 *     (instant, works offline, covers downloaded + previously synced content).
 *  2. Query the server `search3` for full results.
 *  3. Merge, de-duplicating by id and preferring the richer server data while
 *     keeping local-only hits.
 *
 * If no server is reachable (or the network call fails), it degrades gracefully
 * to the local results instead of erroring.
 */
@Singleton
class SearchRepository @Inject constructor(
    private val apiProvider: SubsonicApiProvider,
    private val serverRepository: ServerRepository,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val songDao: SongDao,
) {

    companion object {
        const val LOCAL_LIMIT = 20
        private const val TAG = "SearchRepository"
    }

    suspend fun search(
        query: String,
        artistCount: Int = 20,
        albumCount: Int = 20,
        songCount: Int = 50,
    ): Result<SearchResults> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return Result.success(SearchResults())

        val server = serverRepository.getActiveServer()
        val url = server?.url

        return try {
            // 1) Local cache (instant / offline).
            val localArtists = artistDao.search(trimmed, LOCAL_LIMIT).map { it.toModel(url) }
            val localAlbums = albumDao.search(trimmed, LOCAL_LIMIT).map { it.toModel(url) }
            val localSongs = songDao.searchSongs(trimmed, LOCAL_LIMIT).map { it.toModel(url) }

            // 2) Server search3 (gracefully skipped when offline/rejected).
            var serverArtists: List<Artist> = emptyList()
            var serverAlbums: List<Album> = emptyList()
            var serverSongs: List<Song> = emptyList()
            if (server != null) {
                try {
                    val payload = apiProvider.api(server.url).search3(
                        query = trimmed,
                        artistCount = artistCount,
                        albumCount = albumCount,
                        songCount = songCount,
                    ).data?.result ?: SearchResult3()
                    serverArtists = payload.artist.map { it.toModel(url) }
                    serverAlbums = payload.album.map { it.toModel(url) }
                    serverSongs = payload.song.map { it.toSongModel(url) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    Log.w(TAG, "search3('$trimmed') failed - ${e.message}; falling back to local results")
                }
            }

            // 3) Merge (server data first, then local-only hits, deduped by id).
            val results = SearchResults(
                artists = (serverArtists + localArtists).distinctBy { it.id },
                albums = (serverAlbums + localAlbums).distinctBy { it.id },
                songs = (serverSongs + localSongs).distinctBy { it.id },
            )
            Log.d(
                TAG,
                "search('$trimmed'): local a${localArtists.size}/al${localAlbums.size}/s${localSongs.size} " +
                    "+ server a${serverArtists.size}/al${serverAlbums.size}/s${serverSongs.size} " +
                    "-> total a${results.artists.size}/al${results.albums.size}/s${results.songs.size}",
            )
            Result.success(results)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.e(TAG, "search('$trimmed') failed - ${e::class.simpleName}: ${e.message}", e)
            Result.failure(e)
        }
    }
}
