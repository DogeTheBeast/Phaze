package com.example.phaze2.data.remote.auth

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Appends Subsonic auth + format parameters to every request aimed at the
 * configured server.
 *
 * Token auth:  `u=<user>&t=<token>&s=<salt>&v=1.16.1&c=phaze2&f=json`
 * Legacy auth: `u=<user>&p=<password>&v=1.16.1&c=phaze2&f=json`
 *
 * Binary endpoints (`stream`, `download`, `getCoverArt`) receive the auth
 * parameters but **not** `f=json`, so servers return raw media bytes instead of
 * a JSON wrapper that would confuse ExoPlayer/Coil.
 *
 * Requests to other hosts (e.g. artist image URLs) and requests that already
 * carry explicit auth params are left untouched.
 */
class AuthInterceptor(
    private val authProvider: ServerAuthProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val config = authProvider.current() ?: return chain.proceed(chain.request())
        val request = chain.request()
        if (!request.url.isSameServerAs(config.serverUrl)) return chain.proceed(request)

        val path = request.url.encodedPath
        val isBinary = path.endsWith("/stream") ||
            path.endsWith("/download") ||
            path.endsWith("/getCoverArt")

        val url = request.url.newBuilder().apply {
            if (request.url.queryParameter("u") == null) addQueryParameter("u", config.username)
            if (request.url.queryParameter("v") == null) addQueryParameter("v", SubsonicAuth.API_VERSION)
            if (request.url.queryParameter("c") == null) addQueryParameter("c", SubsonicAuth.CLIENT_NAME)
            if (!isBinary && request.url.queryParameter("f") == null) addQueryParameter("f", "json")
            when {
                config.useLegacyAuth && request.url.queryParameter("p") == null ->
                    addQueryParameter("p", config.legacyPassword.orEmpty())
                !config.useLegacyAuth && request.url.queryParameter("t") == null -> {
                    addQueryParameter("t", config.token.orEmpty())
                    addQueryParameter("s", config.salt.orEmpty())
                }
            }
        }.build()

        return chain.proceed(request.newBuilder().url(url).build())
    }

    private fun HttpUrl.isSameServerAs(serverUrl: String): Boolean {
        val target = serverUrl.toHttpUrlOrNull() ?: return false
        return scheme == target.scheme &&
            host.equals(target.host, ignoreCase = true) &&
            port == target.port
    }
}
