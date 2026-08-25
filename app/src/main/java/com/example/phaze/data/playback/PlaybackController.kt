package com.example.phaze.data.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.phaze.playback.PlaybackService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * App-side facade for background playback. Connects a [MediaController] to the
 * session in [PlaybackService], forwards commands, and mirrors the live state
 * so Player / MiniPlayer / Queue observe the session.
 */
@Singleton
class PlaybackController @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val serviceName = ComponentName(appContext, PlaybackService::class.java)

    private var controller: MediaController? = null
    private var pending: (() -> Unit)? = null

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) = publish()
        override fun onIsPlayingChanged(isPlaying: Boolean) = publish()
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = publish()
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = publish()
        override fun onRepeatModeChanged(repeatMode: Int) = publish()
    }

    init {
        // Position ticker while playing.
        scope.launch {
            while (true) {
                controller?.let { if (it.isPlaying) publish() }
                delay(250)
            }
        }
    }

    /** Starts the service and connects the controller on a background thread. */
    private fun connect(onConnected: () -> Unit) {
        val existing = controller
        if (existing != null) {
            onConnected()
            return
        }
        try {
            ContextCompat.startForegroundService(appContext, Intent(appContext, PlaybackService::class.java))
        } catch (e: Exception) {
            Log.w(TAG, "starting playback service failed: ${e.message}")
        }
        executor.execute {
            try {
                val token = SessionToken(appContext, serviceName)
                val c = MediaController.Builder(appContext, token).buildAsync().get()
                controller = c
                c.addListener(listener)
                scope.launch { onConnected() }
                scope.launch { publish() }
            } catch (e: Exception) {
                Log.e(TAG, "connect: ${e.message}", e)
            }
        }
    }

    // ---- Commands ----

    fun play(items: List<MediaItem>, startIndex: Int = 0) {
        connect {
            val c = controller
            if (c != null && items.isNotEmpty()) {
                c.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0L)
                c.prepare()
                c.play()
                publish()
            }
        }
    }

    fun playPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun seekTo(ms: Long) = controller?.seekTo(ms)
    fun next() = controller?.seekToNextMediaItem()
    fun previous() = controller?.seekToPreviousMediaItem()

    fun playAt(index: Int) {
        controller?.let { if (index in 0 until it.mediaItemCount) { it.seekTo(index, 0L); it.play() } }
    }

    fun removeAt(index: Int) = controller?.removeMediaItem(index)
    fun move(from: Int, to: Int) = controller?.moveMediaItem(from, to)
    fun clear() = controller?.clearMediaItems()

    fun toggleShuffle() = controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    fun toggleRepeat() = controller?.let {
        it.repeatMode = if (it.repeatMode == Player.REPEAT_MODE_ALL) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ALL
    }

    // ---- State ----

    private fun publish() {
        val c = controller ?: return
        val meta = c.currentMediaItem?.mediaMetadata
        val queue = (0 until c.mediaItemCount).map { i ->
            val item = c.getMediaItemAt(i)
            val md = item.mediaMetadata
            QueueTrack(
                id = item.mediaId,
                title = md.title?.toString().orEmpty().ifEmpty { item.mediaId },
                artist = md.artist?.toString().orEmpty(),
                coverArtUrl = md.artworkUri?.toString(),
            )
        }
        _state.value = PlaybackUiState(
            title = meta?.title?.toString().orEmpty().ifEmpty { c.currentMediaItem?.mediaId.orEmpty() },
            artist = meta?.artist?.toString().orEmpty(),
            album = meta?.albumTitle?.toString().orEmpty(),
            albumId = meta?.extras?.getString(PlaybackRepository.KEY_ALBUM_ID),
            artistId = meta?.extras?.getString(PlaybackRepository.KEY_ARTIST_ID),
            coverArtUrl = meta?.artworkUri?.toString(),
            isPlaying = c.isPlaying,
            isShuffle = c.shuffleModeEnabled,
            isRepeat = c.repeatMode == Player.REPEAT_MODE_ALL,
            positionMs = c.currentPosition,
            durationMs = if (c.duration >= 0) c.duration else 0,
            currentIndex = c.currentMediaItemIndex,
            hasCurrent = c.currentMediaItem != null,
            queue = queue,
        )
    }

    private companion object {
        const val TAG = "PlaybackController"
    }
}
