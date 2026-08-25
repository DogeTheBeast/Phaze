package com.example.phaze.data.playback

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.example.phaze.data.model.Song
import com.example.phaze.data.remote.CoverArtUrl
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
                    .setArtworkUri(song.coverArtUrl?.let { upgradeCoverResolution(it, CoverArtUrl.FULL) }?.let { Uri.parse(it) })
                    .setExtras(
                        Bundle().apply {
                            song.albumId?.let { putString(KEY_ALBUM_ID, it) }
                            song.artistId?.let { putString(KEY_ARTIST_ID, it) }
                        }
                    )
                    .build()
            )
            .build()
    }

    suspend fun mediaItems(songs: List<Song>, maxBitRate: Int? = null, format: String? = null): List<MediaItem> =
        songs.mapNotNull { mediaItem(it, maxBitRate, format) }

    /**
     * Rebuilds a `getCoverArt` URL at a higher [size] while keeping the same
     * art id and server base (including subpaths). Falls back to the input if
     * it isn't a cover-art URL.
     */
    private fun upgradeCoverResolution(url: String, size: Int): String {
        val uri = Uri.parse(url)
        val artId = uri.getQueryParameter("id") ?: return url
        return uri.buildUpon()
            .clearQuery()
            .appendQueryParameter("id", artId)
            .appendQueryParameter("size", size.toString())
            .build()
            .toString()
    }

    companion object {
        /** Bundle keys carrying navigation ids inside the MediaItem metadata extras. */
        const val KEY_ALBUM_ID = "albumId"
        const val KEY_ARTIST_ID = "artistId"
    }
}
