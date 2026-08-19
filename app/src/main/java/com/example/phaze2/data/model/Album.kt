package com.example.phaze2.data.model

/**
 * Lightweight album view for UI rails/grids.
 *
 * [coverArtUrl] is the fully-authenticated `getCoverArt` URL (or null when the
 * album has no art / no server configured), ready to feed Coil.
 */
data class Album(
    val id: String,
    val name: String,
    val artistName: String,
    val coverArtUrl: String? = null,
)
