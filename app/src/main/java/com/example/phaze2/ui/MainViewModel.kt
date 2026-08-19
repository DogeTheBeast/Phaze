package com.example.phaze2.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phaze2.data.repository.ServerRepository
import com.example.phaze2.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * App-level state: decides the first screen and restores the saved server's
 * auth material on startup so pings, syncs and streams work after a restart.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
) : ViewModel() {

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
