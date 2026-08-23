package com.example.phaze.ui.screens.album

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phaze.data.model.AlbumDetail
import com.example.phaze.data.playback.PlaybackController
import com.example.phaze.data.playback.PlaybackRepository
import com.example.phaze.data.repository.AlbumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AlbumUiState(
    val detail: AlbumDetail = AlbumDetail(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

/**
 * Album detail screen (mockup album.html). Reads the album id from the nav
 * argument, observer the cached detail, and triggers a `getAlbum` fetch on entry.
 */
@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val playbackRepository: PlaybackRepository,
    private val playbackController: PlaybackController,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val albumId: String = checkNotNull(savedStateHandle["albumId"])

    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AlbumUiState> = combine(
        albumRepository.observe(albumId),
        _isLoading,
        _error,
    ) { detail, loading, error ->
        AlbumUiState(detail = detail, isLoading = loading, errorMessage = error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlbumUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            albumRepository.load(albumId)
                .onSuccess { _isLoading.value = false }
                .onFailure { e ->
                    _isLoading.value = false
                    _error.value = e.message ?: "Couldn't load the album"
                }
        }
    }

    fun toggleStar() {
        viewModelScope.launch { albumRepository.toggleStar(albumId) }
    }

    /** Enqueues the album's tracks and plays from [index]. */
    fun playAt(index: Int) {
        val songs = uiState.value.detail.songs
        viewModelScope.launch {
            val items = playbackRepository.mediaItems(songs)
            if (items.isNotEmpty()) playbackController.play(items, index)
        }
    }
}
