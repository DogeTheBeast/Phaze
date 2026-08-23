package com.example.phaze.data.model

/**
 * Lightweight playlist view for lists/grids (PLAN.md §5, filter page).
 */
data class Playlist(
    val id: String,
    val name: String,
    val songCount: Int = 0,
    val duration: Int = 0,
)
