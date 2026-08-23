package com.example.phaze.data.model

/**
 * Artist detail screen view: header info + the artist's albums, with cover art
 * resolved against the active server (PLAN.md §5, mockup artist.html).
 */
data class ArtistDetail(
    val id: String = "",
    val name: String = "",
    val coverArtUrl: String? = null,
    val albumCount: Int = 0,
    val songCount: Int = 0,
    val starred: Boolean = false,
    val albums: List<Album> = emptyList(),
) {
    val isEmpty: Boolean get() = id.isEmpty()
}
