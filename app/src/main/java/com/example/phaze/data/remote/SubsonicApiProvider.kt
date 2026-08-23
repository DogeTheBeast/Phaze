package com.example.phaze.data.remote

import com.example.phaze.data.remote.auth.AuthInterceptor
import com.example.phaze.data.remote.auth.ServerAuthProvider
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import retrofit2.Retrofit

/**
 * Builds and caches a [SubsonicApi] for the current server base URL.
 *
 * The same authenticated [OkHttpClient] is shared with Coil (cover art) and
 * ExoPlayer (stream URLs) so every request to the server carries the auth params
 * added by [AuthInterceptor]. Switching servers rebuilds the Retrofit instance
 * with the new base URL; auth material is resolved lazily per request.
 */
@Singleton
class SubsonicApiProvider @Inject constructor(
    private val json: Json,
    private val authProvider: ServerAuthProvider,
) {

    @Volatile
    private var cache: Pair<String, SubsonicApi>? = null

    /** Shared client — hand this to Coil/ExoPlayer for authenticated requests. */
    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(authProvider))
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Returns a [SubsonicApi] rooted at [baseUrl] (server root, e.g.
     * `https://navidrome.example.com`). Throws [IllegalArgumentException] on a
     * malformed URL — validate before calling.
     */
    @Synchronized
    fun api(baseUrl: String): SubsonicApi {
        val normalized = normalizeBaseUrl(baseUrl)
        cache?.let { (cachedUrl, api) ->
            if (cachedUrl == normalized) return api
        }
        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(httpClient)
            .addConverterFactory(SubsonicConverterFactory(json))
            .build()
            .create(SubsonicApi::class.java)
            .also { cache = normalized to it }
    }

    /** Strips trailing slashes and re-appends exactly one, as Retrofit requires. */
    private fun normalizeBaseUrl(baseUrl: String): String {
        require(baseUrl.isNotBlank()) { "Server URL must not be blank" }
        return baseUrl.trimEnd('/') + "/"
    }
}
