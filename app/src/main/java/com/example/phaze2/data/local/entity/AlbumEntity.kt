package com.example.phaze2.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.phaze2.data.model.DownloadState

/**
 * Album row fed by `getAlbumList2` / `getAlbum` / `getArtist` / `search3` (AlbumID3).
 *
 * [created] is the server-reported creation timestamp in epoch millis and powers
 * the "recently added" home rail. [playCount]/[lastPlayed] are fed by
 * `getAlbumList2 type=frequent|recent` and power the matching rails.
 */
@Entity(
    tableName = "albums",
    indices = [Index("artistId"), Index("name")],
)
data class AlbumEntity(
    @PrimaryKey val id: String,
    val name: String,
    val artistId: String,
    val artistName: String,
    val year: Int? = null,
    val songCount: Int = 0,
    val duration: Int = 0,
    val coverArt: String? = null,
    val created: Long = 0L,
    val starred: Boolean = false,
    val downloadState: DownloadState = DownloadState.NONE,
    /** Server-reported play count (type=frequent). */
    val playCount: Int = 0,
    /** Epoch millis of the last reported play (type=recent). */
    val lastPlayed: Long? = null,
)
