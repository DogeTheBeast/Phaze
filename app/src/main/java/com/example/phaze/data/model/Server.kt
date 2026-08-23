package com.example.phaze.data.model

/**
 * Result of a successful `ping` — identifies the server and carries the details
 * the app persists as its active server (PLAN.md §1/§4).
 */
data class ServerConnection(
    /** Normalized server root URL (no trailing slash), e.g. `https://music.example.com`. */
    val url: String,
    val username: String,
    /** Server-reported type, e.g. "Navidrome", "Airsonic", "Gonic". */
    val serverType: String? = null,
    val serverVersion: String? = null,
    val openSubsonic: Boolean? = null,
)
