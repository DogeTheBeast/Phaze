package com.example.phaze2.ui.screens.setup

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phaze2.data.local.entity.ServerEntity
import com.example.phaze2.data.model.ServerConnection
import com.example.phaze2.data.remote.SubsonicException
import com.example.phaze2.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ServerSetupUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    /** The currently active (saved) server, if any. */
    val savedServer: ServerEntity? = null,
    val isTesting: Boolean = false,
    val isConnecting: Boolean = false,
    /** Last successful ping result; also carries the URL shown on save. */
    val lastTest: ServerConnection? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class ServerSetupViewModel @Inject constructor(
    private val repository: ServerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServerSetupUiState())
    val uiState: StateFlow<ServerSetupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeActiveServer().collect { server ->
                Log.d(TAG, "observeActiveServer: current=${server?.url}")
                _uiState.update { it.copy(savedServer = server) }
            }
        }
    }

    fun onUrlChange(value: String) =
        _uiState.update { it.copy(serverUrl = value, errorMessage = null, lastTest = null) }

    fun onUsernameChange(value: String) =
        _uiState.update { it.copy(username = value, errorMessage = null, lastTest = null) }

    fun onPasswordChange(value: String) =
        _uiState.update { it.copy(password = value, errorMessage = null, lastTest = null) }

    /** Pings the server with the current form values (no persistence). */
    fun testConnection() {
        val state = _uiState.value
        if (state.isTesting || state.isConnecting) return
        Log.d(TAG, "testConnection: starting url='${state.serverUrl}' user='${state.username}'")
        _uiState.update { it.copy(isTesting = true, errorMessage = null, lastTest = null) }
        viewModelScope.launch {
            repository.testConnection(state.serverUrl, state.username, state.password)
                .onSuccess { result ->
                    Log.d(TAG, "testConnection: success url=${result.url} type=${result.serverType} version=${result.serverVersion}")
                    _uiState.update { it.copy(isTesting = false, lastTest = result) }
                }
                .onFailure { e ->
                    Log.w(TAG, "testConnection: failed - ${e.message}")
                    _uiState.update { it.copy(isTesting = false, errorMessage = e.toUserMessage()) }
                }
        }
    }

    /**
     * Pings (if not already done with the current values), persists the server,
     * then invokes [onConnected] for navigation.
     */
    fun connect(onConnected: () -> Unit) {
        val state = _uiState.value
        if (state.isTesting || state.isConnecting) return
        Log.d(TAG, "connect: starting (reusingExistingTest=${state.lastTest != null})")
        _uiState.update { it.copy(isConnecting = true, errorMessage = null) }
        viewModelScope.launch {
            var result = _uiState.value.lastTest
            if (result == null) {
                val tested = repository.testConnection(
                    _uiState.value.serverUrl,
                    _uiState.value.username,
                    _uiState.value.password,
                ).getOrElse { error ->
                    Log.w(TAG, "connect: test failed - ${error.message}")
                    _uiState.update { it.copy(isConnecting = false, errorMessage = error.toUserMessage()) }
                    return@launch
                }
                _uiState.update { it.copy(lastTest = tested) }
                result = tested
            }
            repository.saveActiveServer(result)
                .onSuccess {
                    Log.i(TAG, "connect: server saved, navigating away")
                    _uiState.update { it.copy(isConnecting = false) }
                    onConnected()
                }
                .onFailure { e ->
                    Log.w(TAG, "connect: save failed - ${e.message}")
                    _uiState.update { it.copy(isConnecting = false, errorMessage = e.toUserMessage()) }
                }
        }
    }

    /** Uses a saved server without re-entering credentials. */
    fun connectSavedServer(onConnected: () -> Unit) {
        val server = _uiState.value.savedServer ?: return
        Log.d(TAG, "connectSavedServer: ${server.url} legacy=${server.useLegacyAuth}")
        viewModelScope.launch {
            repository.restoreAuth(server)
            Log.i(TAG, "connectSavedServer: auth restored, navigating away")
            onConnected()
        }
    }
}

/** Maps repository failures to short, user-facing messages. */
private fun Throwable.toUserMessage(): String = when (this) {
    is SubsonicException -> when (code) {
        SubsonicException.CODE_WRONG_CREDENTIALS,
        SubsonicException.CODE_NOT_AUTHORIZED -> "Wrong username or password"
        SubsonicException.CODE_TOKEN_AUTH_UNSUPPORTED -> "Server rejected this auth method"
        SubsonicException.CODE_INCOMPATIBLE_CLIENT -> "Server is too old for this client"
        SubsonicException.CODE_PARSE_ERROR -> "The server returned an unexpected response"
        else -> message ?: "Server error (code $code)"
    }
    is IOException -> "Couldn't reach the server — check the URL, network, and server status."
    is IllegalArgumentException -> message ?: "Check the details and try again"
    else -> "Something went wrong: ${message ?: "unknown error"}"
}

private const val TAG = "ServerSetup"
