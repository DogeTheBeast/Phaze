package com.example.phaze.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phaze.data.mapper.toModel
import com.example.phaze.data.model.Album
import com.example.phaze.data.repository.LibraryRepository
import com.example.phaze.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val rails: List<HomeRail> = emptyList(),
)

data class HomeRail(
    val key: String,
    val title: String,
    val albums: List<Album>,
)

/**
 * Backs the Home discovery screen (PLAN.md §4 / mockup home.html).
 *
 * Observes the four album rails cached in Room and rebuilds them into
 * [HomeRail]s with real cover-art URLs resolved against the active server.
 * On startup (and whenever the server changes) it triggers a sync that fetches
 * `getAlbumList2` for each rail.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val serverRepository: ServerRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        serverRepository.observeActiveServer(),
        libraryRepository.observeRecentlyAdded(),
        libraryRepository.observeMostPlayed(),
        libraryRepository.observeRecentlyPlayed(),
        libraryRepository.observeRandom(),
    ) { server, recent, frequent, recentPlayed, random ->
        val url = server?.url
        val rails = listOf(
            HomeRail("recent", "Recently added", recent.map { it.toModel(url) }),
            HomeRail("frequent", "Most played", frequent.map { it.toModel(url) }),
            HomeRail("recentPlayed", "Recently played", recentPlayed.map { it.toModel(url) }),
            HomeRail("random", "Random picks", random.map { it.toModel(url) }),
        )
        Log.d(
            TAG,
            "rails emitted: " +
                rails.joinToString(", ") { "${it.title}=${it.albums.size}(covers=${it.albums.count { a -> a.coverArtUrl != null }})" } +
                " server=${url ?: "none"}",
        )
        HomeUiState(rails = rails)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        // Room flows emit the current value immediately, so collecting the
        // active server both syncs on startup and whenever a server connects.
        viewModelScope.launch {
            serverRepository.observeActiveServer().collect { server ->
                if (server != null) {
                    Log.d(TAG, "active server set, syncing home rails: ${server.url}")
                    libraryRepository.syncHomeRails()
                }
            }
        }
    }

    /** Re-fetches the rails (future pull-to-refresh). */
    fun refresh() {
        viewModelScope.launch { libraryRepository.syncHomeRails() }
    }

    private companion object {
        const val TAG = "HomeViewModel"
    }
}
