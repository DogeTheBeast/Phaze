package com.example.phaze.data.repository

import android.util.Log
import com.example.phaze.data.local.dao.ServerDao
import com.example.phaze.data.local.entity.ServerEntity
import com.example.phaze.data.model.ServerConnection
import com.example.phaze.data.remote.SubsonicApiProvider
import com.example.phaze.data.remote.SubsonicException
import com.example.phaze.data.remote.auth.MutableServerAuthProvider
import com.example.phaze.data.remote.auth.ServerAuthConfig
import com.example.phaze.data.remote.auth.SubsonicAuth
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Orchestrates the server connection lifecycle (PLAN.md §4, Phase 1):
 * credential validation via `ping`, persistence of the active server, and
 * re-installing auth material across app restarts.
 *
 * Ping flow: token auth (`u/t/s`) is attempted first; servers that reject it
 * (error codes 40/41) fall back to legacy `p=<password>` auth. The working auth
 * config is left installed on [authProvider] so every subsequent request
 * (sync, streams, cover art) is authenticated.
 */
@Singleton
class ServerRepository @Inject constructor(
    private val apiProvider: SubsonicApiProvider,
    private val authProvider: MutableServerAuthProvider,
    private val serverDao: ServerDao,
) {

    fun observeActiveServer(): Flow<ServerEntity?> = serverDao.observeActive()

    suspend fun getActiveServer(): ServerEntity? = serverDao.getActive()

    /**
     * Validates credentials by pinging the server. On success returns the
     * normalized URL + server info; failures are [Result.failure] carrying an
     * [IllegalArgumentException] for invalid input or an
     * [com.example.phaze.data.remote.SubsonicException] / [java.io.IOException]
     * for connectivity/auth problems.
     */
    suspend fun testConnection(url: String, username: String, password: String): Result<ServerConnection> {
        Log.d(TAG, "testConnection: trying connection, user='$username' url='${url.trim()}'")
        val baseUrl = normalizeUrl(url)
        if (baseUrl == null) {
            Log.w(TAG, "testConnection: invalid server URL input='${url.trim()}'")
            return Result.failure(
                IllegalArgumentException("Enter a valid server URL, e.g. https://music.example.com")
            )
        }
        if (username.isBlank()) {
            Log.w(TAG, "testConnection: blank username")
            return Result.failure(IllegalArgumentException("Enter your username"))
        }
        if (password.isBlank()) {
            Log.w(TAG, "testConnection: blank password")
            return Result.failure(IllegalArgumentException("Enter your password"))
        }

        return try {
            val salt = SubsonicAuth.generateSalt()
            val token = SubsonicAuth.computeToken(password, salt)
            Log.d(TAG, "testConnection: built token auth (salt=$salt) for '$baseUrl'")
            authProvider.config = ServerAuthConfig.tokenAuth(baseUrl, username, token, salt)

            val response = try {
                Log.d(TAG, "testConnection: pinging $baseUrl with token auth")
                apiProvider.api(baseUrl).ping().also {
                    Log.d(
                        TAG,
                        "testConnection: token ping OK status=${it.status} type=${it.serverType} version=${it.serverVersion} openSubsonic=${it.openSubsonic}",
                    )
                }
            } catch (e: SubsonicException) {
                Log.w(TAG, "testConnection: token auth failed code=${e.code} msg='${e.message}'")
                when (e.code) {
                    SubsonicException.CODE_TOKEN_AUTH_UNSUPPORTED,
                    SubsonicException.CODE_WRONG_CREDENTIALS -> {
                        // Old server without token support — retry with legacy auth.
                        Log.i(TAG, "testConnection: falling back to legacy password auth")
                        authProvider.config = ServerAuthConfig.legacy(baseUrl, username, password)
                        apiProvider.api(baseUrl).ping().also {
                            Log.d(
                                TAG,
                                "testConnection: legacy ping OK status=${it.status} type=${it.serverType} version=${it.serverVersion} openSubsonic=${it.openSubsonic}",
                            )
                        }
                    }
                    else -> throw e
                }
            }

            Result.success(
                ServerConnection(
                    url = baseUrl,
                    username = username,
                    serverType = response.serverType,
                    serverVersion = response.serverVersion,
                    openSubsonic = response.openSubsonic,
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.e(TAG, "testConnection: failed - ${e::class.simpleName}: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Persists the last successfully-tested connection as the active server.
     * The auth material (token+salt, or legacy password) is read from the
     * provider, which [testConnection] left in its working state.
     */
    suspend fun saveActiveServer(result: ServerConnection): Result<Unit> = try {
        val config = authProvider.current()
            ?: run {
                Log.e(TAG, "saveActiveServer: no auth config present - run testConnection first")
                error("No authenticated connection to save — run testConnection first")
            }
        Log.i(
            TAG,
            "saveActiveServer: persisting server url='${result.url}' username='${result.username}' type=${result.serverType} legacy=${config.useLegacyAuth}",
        )
        serverDao.upsert(
            ServerEntity(
                id = 0,
                url = result.url,
                username = result.username,
                token = config.token,
                salt = config.salt,
                useLegacyAuth = config.useLegacyAuth,
                legacyPassword = if (config.useLegacyAuth) config.legacyPassword else null,
                serverType = result.serverType,
            )
        )
        Log.d(TAG, "saveActiveServer: saved")
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Log.e(TAG, "saveActiveServer: failed - ${e::class.simpleName}: ${e.message}", e)
        Result.failure(e)
    }

    /** Re-installs auth material from the saved server (app restart / saved-server connect). */
    fun restoreAuth(server: ServerEntity) {
        Log.i(TAG, "restoreAuth: server='${server.url}' legacy=${server.useLegacyAuth}")
        authProvider.config = if (server.useLegacyAuth) {
            ServerAuthConfig.legacy(server.url, server.username, server.legacyPassword.orEmpty())
        } else {
            ServerAuthConfig.tokenAuth(server.url, server.username, server.token.orEmpty(), server.salt.orEmpty())
        }
    }

    /**
     * Pings the currently saved/active server using its stored credentials.
     * Used by the Settings "Test connection" row.
     */
    suspend fun checkActiveServer(): Result<ServerConnection> {
        val server = serverDao.getActive()
            ?: return Result.failure(IllegalStateException("No server configured"))
        restoreAuth(server)
        return try {
            val response = apiProvider.api(server.url).ping()
            Log.d(TAG, "checkActiveServer: '${server.url}' OK type=${response.serverType} version=${response.serverVersion}")
            Result.success(
                ServerConnection(
                    url = server.url,
                    username = server.username,
                    serverType = response.serverType,
                    serverVersion = response.serverVersion,
                    openSubsonic = response.openSubsonic,
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.w(TAG, "checkActiveServer: failed - ${e.message}")
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "ServerRepository"

        /**
         * Normalizes user input into a server root URL: trims whitespace,
         * defaults to `https://` when no scheme is given, drops query/fragment
         * and trailing slashes, and strips an accidental `/rest` suffix.
         * Returns null for invalid input.
         */
        fun normalizeUrl(raw: String): String? {

            var url = raw.trim()
            if (url.isEmpty()) return null
            if (!url.contains("://")) url = "https://$url"
            val parsed = url.toHttpUrlOrNull() ?: return null
            if (parsed.scheme != "http" && parsed.scheme != "https") return null
            var base = parsed.newBuilder()
                .query(null)
                .fragment(null)
                .build()
                .toString()
                .trimEnd('/')
            if (base.endsWith("/rest")) base = base.removeSuffix("/rest")
            return base
        }
    }
}
