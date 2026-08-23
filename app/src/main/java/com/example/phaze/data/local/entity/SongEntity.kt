package com.example.phaze.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.phaze.data.model.DownloadState

/**
 * Track row fed by `getAlbum` / `getSongs` / `search3` / `getPlaylist` (Child).
 *
 * [localPath] points at the file under `filesDir/downloads/` once the track is
 * downloaded; playback prefers it over `stream?id=` (PLAN.md §7).
 */
@Entity(
    tableName = "songs",
    indices = [Index("albumId"), Index("artistId")],
)
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val albumId: String,
    val artistId: String,
    val artistName: String,
    val track: Int = 0,
    val duration: Int = 0,
    val bitrate: Int? = null,
    /** File extension, e.g. "mp3", "flac", "ogg" (Child.suffix). */
    val format: String? = null,
    /** MIME type, e.g. "audio/mpeg" (Child.contentType). */
    val contentType: String? = null,
    /** Size in bytes; used for storage accounting. */
    val size: Long = 0L,
    val coverArt: String? = null,
    val starred: Boolean = false,
    val downloadState: DownloadState = DownloadState.NONE,
    val localPath: String? = null,
)
