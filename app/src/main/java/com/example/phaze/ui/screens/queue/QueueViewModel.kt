package com.example.phaze.ui.screens.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phaze.data.playback.PlaybackController
import com.example.phaze.data.playback.QueueTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class QueueUiState(
    val items: List<QueueTrack> = emptyList(),
    val currentIndex: Int = -1,
) {
    val isEmpty: Boolean get() = items.isEmpty()
}

/**
 * Playback queue screen (PLAN.md §3/§7, mockup queue.html), backed by the real
 * ExoPlayer queue through [PlaybackController]. Reorder / remove / clear / play
 * operate directly on the live queue.
 */
@HiltViewModel
class QueueViewModel @Inject constructor(
    private val playbackController: PlaybackController,
) : ViewModel() {

    val uiState: StateFlow<QueueUiState> = playbackController.state
        .map { s -> QueueUiState(items = s.queue, currentIndex = s.currentIndex) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QueueUiState())

    fun playAt(index: Int) = playbackController.playAt(index)
    fun removeAt(index: Int) = playbackController.removeAt(index)
    fun move(from: Int, to: Int) = playbackController.move(from, to)
    fun clear() = playbackController.clear()

    fun saveAsPlaylist() {
        // TODO(Phase 3): create/update a server playlist from the current queue.
    }
}
