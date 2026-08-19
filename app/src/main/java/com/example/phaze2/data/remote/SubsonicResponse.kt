package com.example.phaze2.data.remote

import kotlinx.serialization.Serializable

/**
 * Typed wrapper around the OpenSubsonic response envelope.
 *
 * The wire format is:
 * ```
 * { "subsonic-response": {
 *     "status": "ok" | "failed",
 *     "version": "1.16.1",
 *     "type": "Navidrome", "serverVersion": "...", "openSubsonic": true,
 *     "error": { "code": 40, "message": "..." },   // only when status=failed
 *     <payload keys, e.g. "artists" / "albumList2" / "album" / ...>
 * } }
 * ```
 *
 * Payload keys differ per endpoint, so [data] is decoded by
 * [SubsonicConverterFactory] from whatever remains after stripping the envelope
 * keys. Network/HTTP failures surface as OkHttp [java.io.IOException]s; server
 * errors (`status = "failed"`) are thrown as [SubsonicException] with [error].
 */
data class SubsonicResponse<T>(
    val status: String,
    val version: String,
    /** Server type, e.g. "Navidrome", "Airsonic", "Gonic". */
    val serverType: String? = null,
    val serverVersion: String? = null,
    val openSubsonic: Boolean? = null,
    val error: SubsonicError? = null,
    val data: T? = null,
) {
    val isOk: Boolean get() = status == "ok"
}

/** Error body attached to a failed response (see Subsonic API spec codes). */
@Serializable
data class SubsonicError(
    val code: Int = 0,
    val message: String = "",
)
