package com.example.phaze.data.remote.auth

/**
 * Snapshot of the authentication material for the active server.
 *
 * Produced by the settings layer from [com.example.phaze.data.local.entity.ServerEntity]
 * plus the password kept in secure storage (see PLAN.md §13). For token auth only
 * the derived `token = MD5(password + salt)` is kept; for legacy servers the raw
 * password is required because the server has no way to verify a token.
 */
data class ServerAuthConfig(
    /** Server root URL, e.g. `https://navidrome.example.com`. */
    val serverUrl: String,
    val username: String,
    /** Hex MD5(password + salt) — token auth (OpenSubsonic default). */
    val token: String? = null,
    /** Random salt the token was derived with. */
    val salt: String? = null,
    /** Raw password — legacy auth only. */
    val legacyPassword: String? = null,
    val useLegacyAuth: Boolean = false,
) {
    companion object {
        fun tokenAuth(serverUrl: String, username: String, token: String, salt: String) =
            ServerAuthConfig(
                serverUrl = serverUrl,
                username = username,
                token = token,
                salt = salt,
                legacyPassword = null,
                useLegacyAuth = false,
            )

        fun legacy(serverUrl: String, username: String, password: String) =
            ServerAuthConfig(
                serverUrl = serverUrl,
                username = username,
                token = null,
                salt = null,
                legacyPassword = password,
                useLegacyAuth = true,
            )
    }
}
