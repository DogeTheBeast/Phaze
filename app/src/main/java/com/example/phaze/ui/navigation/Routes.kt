package com.example.phaze.ui.navigation

object Routes {
    const val SETUP = "setup"
    const val HOME = "home"
    const val LIBRARY = "library"
    const val FILTER = "filter/{type}"
    const val ARTIST = "artist/{artistId}"
    const val ALBUM = "album/{albumId}"
    const val PLAYER = "player"
    const val QUEUE = "queue"
    const val DOWNLOADS = "downloads"
    const val SEARCH = "search"
    const val SETTINGS = "settings"

    fun artist(artistId: String) = "artist/$artistId"
    fun album(albumId: String) = "album/$albumId"
    fun filter(type: String) = "filter/$type"
}
