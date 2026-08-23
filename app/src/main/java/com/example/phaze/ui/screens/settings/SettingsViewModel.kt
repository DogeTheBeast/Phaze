package com.example.phaze.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phaze.data.local.entity.ServerEntity
import com.example.phaze.data.model.ServerConnection
import com.example.phaze.data.repository.ServerRepository
import com.example.phaze.data.repository.SettingsPreferences
import com.example.phaze.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ConnectionState {
    data object Idle : ConnectionState
    data object Testing : ConnectionState
    data class Ok(val result: ServerConnection) : ConnectionState
    data class Error(val message: String) : ConnectionState
}

/** Settings screen (PLAN.md §1/§8, mockup settings.html). */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val serverRepository: ServerRepository,
) : ViewModel() {

    val settings: StateFlow<SettingsPreferences> =
        settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsPreferences())

    val server: StateFlow<ServerEntity?> =
        serverRepository.observeActiveServer().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _connection = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connection.asStateFlow()

    fun testConnection() {
        _connection.value = ConnectionState.Testing
        viewModelScope.launch {
            serverRepository.checkActiveServer()
                .onSuccess { _connection.value = ConnectionState.Ok(it) }
                .onFailure { _connection.value = ConnectionState.Error(it.message ?: "Connection failed") }
        }
    }

    fun setGaplessPlayback(enabled: Boolean) = launchUpdate { settingsRepository.setGaplessPlayback(enabled) }
    fun setDownloadOnWifiOnly(enabled: Boolean) = launchUpdate { settingsRepository.setDownloadOnWifiOnly(enabled) }
    fun setAutoDownloadStarred(enabled: Boolean) = launchUpdate { settingsRepository.setAutoDownloadStarred(enabled) }
    fun setOfflineMode(enabled: Boolean) = launchUpdate { settingsRepository.setOfflineMode(enabled) }
    fun setScrobble(enabled: Boolean) = launchUpdate { settingsRepository.setScrobble(enabled) }
    fun setShowOfflineBanner(enabled: Boolean) = launchUpdate { settingsRepository.setShowOfflineBanner(enabled) }
    fun setAccent(key: String) = launchUpdate { settingsRepository.setAccentKey(key) }

    private fun launchUpdate(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
