package com.example.phaze.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phaze.data.playback.PlaybackController
import com.example.phaze.data.playback.PlaybackUiState
import com.example.phaze.data.repository.ServerRepository
import com.example.phaze.data.repository.SettingsRepository
import com.example.phaze.ui.navigation.Routes
import com.example.phaze.ui.theme.Accent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * App-level state: decides the first screen, restores the saved server's auth
 * material on startup, and exposes the selected accent color for the theme.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    settingsRepository: SettingsRepository,
    private val playbackController: PlaybackController,
) : ViewModel() {

    /** Live playback session, shown by the mini player. */
    val playbackState: StateFlow<PlaybackUiState> = playbackController.state

    fun playPause() = playbackController.playPause()


    /** The currently selected accent, fed into [com.example.phaze.ui.theme.PhazeTheme]. */
    val accent: StateFlow<Accent> = settingsRepository.settings
        .map { Accent.fromKey(it.accentKey) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Accent.BLUE)

    /**
     * First destination: SETUP until a server exists, HOME otherwise.
     * `null` while the DB read is in flight — the UI shows a blank scaffold
     * instead of flashing the wrong screen.
     */
    val startDestination: StateFlow<String?> = serverRepository.observeActiveServer()
        .map { server -> if (server == null) Routes.SETUP else Routes.HOME }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            serverRepository.observeActiveServer().firstOrNull()?.let {
                serverRepository.restoreAuth(it)
            }
        }
    }
}
