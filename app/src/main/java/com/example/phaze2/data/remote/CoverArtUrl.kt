package com.example.phaze2.data.remote

import android.net.Uri

/**
 * Builds the `getCoverArt` URL for a given cover-art id and pixel size.
 *
 * The URL is a plain REST call; the [com.example.phaze2.data.remote.auth.AuthInterceptor]
 * attached to the shared OkHttpClient appends the auth parameters at request
 * time, so Coil requests through that client are automatically authenticated.
 */
object CoverArtUrl {

    const val RAIL = 512
    const val FULL = 1024

    fun of(serverUrl: String, coverArtId: String, size: Int): String {
        val base = serverUrl.trimEnd('/')
        return buildString {
            append(base)
            append("/rest/getCoverArt?id=")
            append(Uri.encode(coverArtId))
            append("&size=")
            append(size)
        }
    }
}
