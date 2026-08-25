package com.example.phaze.ui.screens.filter

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phaze.data.model.Album
import com.example.phaze.data.model.Artist
import com.example.phaze.data.model.Playlist
import com.example.phaze.data.model.Song
import com.example.phaze.data.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FilterUiState(
    val type: FilterType = FilterType.ALL_ALBUMS,
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val songs: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = true,
)

/**
 * Shared filter/browse screen reached from Home filter tiles and Search browse
 * tiles. Renders albums/artists/songs/playlists per [FilterType] and syncs the
 * relevant endpoint on entry.
 */
@HiltViewModel
class FilterViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val type = FilterType.fromKey(savedStateHandle["type"])
    private val _isLoading = MutableStateFlow(true)

    // Random albums are snapshotted once per visit (ViewModel lifetime) so the
    // page keeps the same set when navigating back — it only re-shuffles when
    // the Random tile is reopened from Home.
    private val randomAlbums = MutableStateFlow<List<Album>>(emptyList())

    private val content: kotlinx.coroutines.flow.Flow<FilterUiState> = when (type.kind) {
        FilterType.Kind.ALBUMS -> {
            val albums: kotlinx.coroutines.flow.Flow<List<Album>> =
                if (type == FilterType.RANDOM) randomAlbums else libraryRepository.observeAlbums(type.key)
            combine(albums, _isLoading) { albums, loading ->
                FilterUiState(type = type, albums = albums, isLoading = loading)
            }
        }
        FilterType.Kind.ARTISTS ->
            combine(libraryRepository.observeArtists(), _isLoading) { artists, loading ->
                FilterUiState(type = type, artists = artists, isLoading = loading)
            }
        FilterType.Kind.SONGS ->
            combine(libraryRepository.observeSongs(), _isLoading) { songs, loading ->
                FilterUiState(type = type, songs = songs, isLoading = loading)
            }
        FilterType.Kind.PLAYLISTS ->
            combine(libraryRepository.observePlaylists(), _isLoading) { playlists, loading ->
                FilterUiState(type = type, playlists = playlists, isLoading = loading)
            }
        FilterType.Kind.EMPTY ->
            _isLoading.map { FilterUiState(type = type, isLoading = false) }
    }

    val uiState: StateFlow<FilterUiState> = content.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        FilterUiState(type = type, isLoading = true),
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            libraryRepository.syncFilter(type.key)
            if (type == FilterType.RANDOM) {
                randomAlbums.value = libraryRepository.getRandomAlbums()
            }
            _isLoading.value = false
        }
    }
}
