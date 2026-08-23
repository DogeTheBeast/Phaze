package com.example.phaze.ui.screens.filter

/**
 * The shared "filter / browse" destinations, reachable from both the Home
 * filter grid and the Search browse grid. [key] is the nav-arg value.
 */
enum class FilterType(val key: String, val title: String, val kind: Kind) {
    RECENTLY_ADDED("added", "Recently added", Kind.ALBUMS),
    RECENTLY_PLAYED("recent", "Recently played", Kind.ALBUMS),
    MOST_PLAYED("frequent", "Most played", Kind.ALBUMS),
    LEAST_PLAYED("least", "Least played", Kind.ALBUMS),
    NEWLY_STARRED("newly-starred", "Newly starred", Kind.ALBUMS),
    STARRED("starred", "Starred", Kind.ALBUMS),
    RANDOM("random", "Random", Kind.ALBUMS),
    DOWNLOADED("downloaded", "Downloaded", Kind.ALBUMS),
    ALL_ALBUMS("albums", "Albums", Kind.ALBUMS),
    ARTISTS("artists", "Artists", Kind.ARTISTS),
    SONGS("songs", "Songs", Kind.SONGS),
    PLAYLISTS("playlists", "Playlists", Kind.PLAYLISTS),
    GENRES("genres", "Genres", Kind.EMPTY),
    YEARS("years", "Years", Kind.EMPTY),
    MOODS("moods", "Moods", Kind.EMPTY),
    ;

    enum class Kind { ALBUMS, ARTISTS, SONGS, PLAYLISTS, EMPTY }

    companion object {
        fun fromKey(key: String?): FilterType = entries.firstOrNull { it.key == key } ?: ALL_ALBUMS
    }
}
