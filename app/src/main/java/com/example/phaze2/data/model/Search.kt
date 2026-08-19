package com.example.phaze2.data.model

/**
 * Grouped results of `search3` — artists, albums and songs (PLAN.md §5).
 */
data class SearchResults(
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val songs: List<Song> = emptyList(),
) {
    val isEmpty: Boolean
        get() = artists.isEmpty() && albums.isEmpty() && songs.isEmpty()
}

data class Artist(
    val id: String,
    val name: String,
    val coverArtUrl: String? = null,
)

data class Song(
    val id: String,
    val title: String,
    val artistName: String,
    val albumName: String? = null,
    val albumId: String? = null,
    val duration: Int = 0,
    val coverArtUrl: String? = null,
)
