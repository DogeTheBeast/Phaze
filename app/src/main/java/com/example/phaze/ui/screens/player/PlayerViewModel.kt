package com.example.phaze.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phaze.data.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class PlayerUiState(
    val title: String = "Nothing playing",
    val artist: String = "",
    val album: String = "",
    val coverArtUrl: String? = null,
    val isPlaying: Boolean = false,
    val isShuffle: Boolean = false,
    val isRepeat: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val quality: String = "FLAC · 1411 kbps",
)

/**
 * Now-playing screen driven by the live Media3 session through
 * [PlaybackController] (PLAN.md §7, mockup player.html).
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackController: PlaybackController,
) : ViewModel() {

    val uiState: StateFlow<PlayerUiState> = playbackController.state
        .map { s ->
            PlayerUiState(
                title = s.title.ifEmpty { "Nothing playing" },
                artist = s.artist,
                album = s.album,
                coverArtUrl = s.coverArtUrl,
                isPlaying = s.isPlaying,
                isShuffle = s.isShuffle,
                isRepeat = s.isRepeat,
                positionMs = s.positionMs,
                durationMs = s.durationMs,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerUiState())

    fun playPause() = playbackController.playPause()
    fun toggleShuffle() = playbackController.toggleShuffle()
    fun toggleRepeat() = playbackController.toggleRepeat()
    fun seekTo(ms: Long) = playbackController.seekTo(ms)
    fun next() = playbackController.next()
    fun previous() = playbackController.previous()
}
