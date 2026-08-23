package com.example.phaze.ui.screens.artist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phaze.data.model.ArtistDetail
import com.example.phaze.data.repository.ArtistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ArtistUiState(
    val detail: ArtistDetail = ArtistDetail(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

/**
 * Artist detail screen (mockup artist.html). Reads the artist id from the nav
 * argument and triggers a `getArtist` fetch on entry.
 */
@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val artistRepository: ArtistRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val artistId: String = checkNotNull(savedStateHandle["artistId"])

    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ArtistUiState> = combine(
        artistRepository.observe(artistId),
        _isLoading,
        _error,
    ) { detail, loading, error ->
        ArtistUiState(detail = detail, isLoading = loading, errorMessage = error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ArtistUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            artistRepository.load(artistId)
                .onSuccess { _isLoading.value = false }
                .onFailure { e ->
                    _isLoading.value = false
                    _error.value = e.message ?: "Couldn't load the artist"
                }
        }
    }

    fun toggleStar() {
        viewModelScope.launch { artistRepository.toggleStar(artistId) }
    }
}
