package com.example.phaze2.data.remote

import java.io.IOException

/**
 * Thrown when the server answers with a Subsonic error (`status = "failed"`).
 *
 * Transport failures (DNS, TLS, HTTP 4xx/5xx) surface as plain [IOException]s
 * from OkHttp; [SubsonicException] carries the server-reported [code] from
 * [SubsonicError] so callers can distinguish e.g. wrong credentials (40) from
 * token auth unsupported (41) and fall back to legacy auth.
 */
class SubsonicException(
    val code: Int,
    override val message: String,
    val serverVersion: String? = null,
    cause: Throwable? = null,
) : IOException(message, cause) {

    companion object {
        /** Server returned a non-JSON / malformed body. */
        const val CODE_PARSE_ERROR = -1

        // Subsonic API error codes
        const val CODE_GENERIC = 0
        const val CODE_MISSING_PARAMETER = 10
        const val CODE_INCOMPATIBLE_CLIENT = 20
        const val CODE_INCOMPATIBLE_SERVER = 30
        const val CODE_WRONG_CREDENTIALS = 40
        const val CODE_TOKEN_AUTH_UNSUPPORTED = 41
        const val CODE_NOT_AUTHORIZED = 50
        const val CODE_TRIAL_EXPIRED = 60
        const val CODE_DATA_NOT_FOUND = 70
    }
}
