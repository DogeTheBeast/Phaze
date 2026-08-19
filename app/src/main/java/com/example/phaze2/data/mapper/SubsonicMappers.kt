package com.example.phaze2.data.mapper

import com.example.phaze2.data.local.entity.AlbumEntity
import com.example.phaze2.data.local.entity.ArtistEntity
import com.example.phaze2.data.local.entity.PlaylistEntity
import com.example.phaze2.data.local.entity.PlaylistSongCrossRef
import com.example.phaze2.data.local.entity.SongEntity
import com.example.phaze2.data.model.DownloadState
import com.example.phaze2.data.remote.dto.AlbumID3
import com.example.phaze2.data.remote.dto.ArtistID3
import com.example.phaze2.data.remote.dto.Child
import com.example.phaze2.data.remote.dto.PlaylistWithSongs
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * DTO → Room entity mappers used by the library sync (PLAN.md §5/§2).
 *
 * Sync never loses local state: [Child.toEntity] accepts the existing row and
 * carries over `downloadState`/`localPath`/`starred`, so re-fetching an album
 * does not wipe downloads or star toggles.
 */

fun ArtistID3.toEntity(): ArtistEntity = ArtistEntity(
    id = id,
    name = name,
    albumCount = albumCount,
    coverArt = coverArt,
)

fun AlbumID3.toEntity(): AlbumEntity = AlbumEntity(
    id = id,
    name = name,
    artistId = artistId,
    artistName = artist,
    year = year,
    songCount = songCount,
    duration = duration,
    coverArt = coverArt,
    created = created?.parseSubsonicDate() ?: 0L,
    starred = starred != null,
    downloadState = DownloadState.NONE,
    playCount = playCount ?: 0,
    lastPlayed = null,
)

fun Child.toEntity(
    albumId: String? = null,
    artistId: String? = null,
    existing: SongEntity? = null,
): SongEntity = SongEntity(
    id = id,
    title = title ?: "",
    albumId = albumId ?: parent ?: "",
    artistId = artistId ?: "",
    artistName = artist ?: "",
    track = track ?: 0,
    duration = duration ?: 0,
    bitrate = bitRate,
    format = suffix,
    contentType = contentType,
    size = size ?: 0L,
    coverArt = coverArt,
    starred = existing?.starred ?: (starred != null),
    downloadState = existing?.downloadState ?: DownloadState.NONE,
    localPath = existing?.localPath,
)

fun PlaylistWithSongs.toEntity(): PlaylistEntity = PlaylistEntity(
    id = id,
    name = name ?: "",
    songCount = songCount,
    duration = duration,
    created = created?.parseSubsonicDate() ?: 0L,
    isPublic = isPublic,
)

/** Builds ordered cross-refs for this playlist's `entry` tracks. */
fun PlaylistWithSongs.toCrossRefs(): List<PlaylistSongCrossRef> =
    entry.mapIndexed { index, song -> PlaylistSongCrossRef(playlistId = id, songId = song.id, position = index) }

/**
 * Parses a Subsonic timestamp. Servers vary: ISO-8601 with `Z`/offset, naive
 * local time, or a bare date. Naive values are interpreted as UTC.
 */
private fun String.parseSubsonicDate(): Long? = try {
    Instant.parse(this).toEpochMilli()
} catch (_: DateTimeException) {
    try {
        LocalDateTime.parse(this).toInstant(ZoneOffset.UTC).toEpochMilli()
    } catch (_: DateTimeException) {
        try {
            LocalDate.parse(this).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        } catch (_: DateTimeException) {
            null
        }
    }
}
