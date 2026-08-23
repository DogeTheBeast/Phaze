package com.example.phaze.data.model

/**
 * Album detail screen view: album header metadata + its track list, with cover
 * art resolved against the active server (PLAN.md §5, mockup album.html).
 */
data class AlbumDetail(
    val id: String = "",
    val name: String = "",
    val artistName: String = "",
    val artistId: String = "",
    val year: Int? = null,
    val songCount: Int = 0,
    val duration: Int = 0,
    val starred: Boolean = false,
    val coverArtUrl: String? = null,
    val songs: List<Song> = emptyList(),
) {
    val isEmpty: Boolean get() = id.isEmpty()
}
