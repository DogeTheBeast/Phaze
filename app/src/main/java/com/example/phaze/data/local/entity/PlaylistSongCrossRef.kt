package com.example.phaze.data.local.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Ordering join table between playlists and their member songs.
 *
 * [position] is the 0-based index within the playlist. No foreign keys are
 * declared on purpose: playlists and songs sync independently, and a song may
 * legitimately not exist in the local DB yet when its playlist arrives.
 * The repository is responsible for clearing cross-refs when deleting a playlist.
 */
@Entity(
    tableName = "playlist_song_cross_ref",
    primaryKeys = ["playlistId", "songId"],
    indices = [Index("songId")],
)
data class PlaylistSongCrossRef(
    val playlistId: String,
    val songId: String,
    val position: Int = 0,
)
