package com.example.phaze2.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Playlist row fed by `getPlaylists` / `getPlaylist`.
 *
 * The member tracks live in [PlaylistSongCrossRef]. Note the SQL column is
 * `is_public`: `public` is a Kotlin keyword and would not compile as a property.
 */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val songCount: Int = 0,
    val duration: Int = 0,
    val created: Long = 0L,
    @ColumnInfo(name = "is_public")
    val isPublic: Boolean = false,
)
