package com.example.phaze2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Artist row fed by `getArtists` / `getArtist` / `search3` (ArtistID3).
 *
 * `coverArt` is the Subsonic cover-art id (`ar-<id>`), resolved through
 * `getCoverArt?id=...` when rendering.
 */
@Entity(tableName = "artists")
data class ArtistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val albumCount: Int = 0,
    val coverArt: String? = null,
)
