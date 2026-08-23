package com.example.phaze.data.remote.auth

/**
 * Supplies the auth material for the currently active server to interceptors.
 *
 * Kept as an interface so [AuthInterceptor] and [com.example.phaze.data.remote.SubsonicApiProvider]
 * don't depend on storage details; the settings layer writes to the
 * [MutableServerAuthProvider] singleton after server setup / login.
 */
fun interface ServerAuthProvider {
    fun current(): ServerAuthConfig?
}

/**
 * Mutable implementation backed by a volatile field — safe to read from OkHttp
 * interceptor threads while the settings layer swaps servers.
 */
class MutableServerAuthProvider : ServerAuthProvider {

    @Volatile
    var config: ServerAuthConfig? = null

    override fun current(): ServerAuthConfig? = config

    /** Clears credentials (logout / server switch). */
    fun clear() {
        config = null
    }
}
