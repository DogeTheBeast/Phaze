package com.example.phaze.data.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.example.phaze.data.model.Song
import com.example.phaze.data.repository.ServerRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds playable [MediaItem]s for songs, sourcing bytes from the Subsonic
 * `stream` endpoint (transcoded via `maxBitRate`/`format`) or from a local file
 * when the song is downloaded (PLAN.md §7).
 *
 * Stream URLs are plain REST calls; ExoPlayer's data source is backed by the
 * shared OkHttpClient, whose AuthInterceptor attaches the credentials, so
 * playback requests are authenticated automatically.
 */
@Singleton
class PlaybackRepository @Inject constructor(
    private val serverRepository: ServerRepository,
) {

    /** `…/rest/stream?id=…&maxBitRate=…&format=…` against [serverUrl]. */
    fun streamUri(
        serverUrl: String,
        songId: String,
        maxBitRate: Int? = null,
        format: String? = null,
    ): Uri {
        val builder = Uri.parse("${serverUrl.trimEnd('/')}/rest/stream").buildUpon()
            .appendQueryParameter("id", songId)
        maxBitRate?.let { builder.appendQueryParameter("maxBitRate", it.toString()) }
        format?.let { builder.appendQueryParameter("format", it) }
        return builder.build()
    }

    suspend fun mediaItem(
        song: Song,
        maxBitRate: Int? = null,
        format: String? = null,
    ): MediaItem? {
        val server = serverRepository.getActiveServer() ?: return null
        val uri = song.localPath?.let { local -> File(local).takeIf { it.exists() }?.toURI()?.toString() }
            ?: streamUri(server.url, song.id, maxBitRate, format).toString()
        return MediaItem.Builder()
            .setUri(uri)
            .setMediaId(song.id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artistName)
                    .setAlbumTitle(song.albumName)
                    .setArtworkUri(song.coverArtUrl?.let { Uri.parse(it) })
                    .build()
            )
            .build()
    }

    suspend fun mediaItems(songs: List<Song>, maxBitRate: Int? = null, format: String? = null): List<MediaItem> =
        songs.mapNotNull { mediaItem(it, maxBitRate, format) }
}
