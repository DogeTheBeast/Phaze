package com.example.phaze.data.mapper

import com.example.phaze.data.model.Album
import com.example.phaze.data.model.Artist
import com.example.phaze.data.model.Song
import com.example.phaze.data.remote.CoverArtUrl
import com.example.phaze.data.remote.dto.AlbumID3
import com.example.phaze.data.remote.dto.ArtistID3
import com.example.phaze.data.remote.dto.Child
import com.example.phaze.data.local.entity.ArtistEntity
import com.example.phaze.data.local.entity.SongEntity

/**
 * DTO → domain mappings for search results. Covers resolve against [serverUrl]
 * (null when no server is configured).
 */

fun ArtistID3.toModel(serverUrl: String?): Artist = Artist(
    id = id,
    name = name,
    coverArtUrl = coverArt?.let { serverUrl?.let { s -> CoverArtUrl.of(s, it, 256) } },
)

fun AlbumID3.toModel(serverUrl: String?): Album = Album(
    id = id,
    name = name,
    artistName = artist,
    year = year,
    coverArtUrl = coverArt?.let { serverUrl?.let { s -> CoverArtUrl.of(s, it, CoverArtUrl.RAIL) } },
)

fun Child.toSongModel(serverUrl: String?): Song = Song(
    id = id,
    title = title ?: "",
    artistName = artist ?: "",
    albumName = album,
    albumId = albumId ?: parent,
    duration = duration ?: 0,
    coverArtUrl = coverArt?.let { serverUrl?.let { s -> CoverArtUrl.of(s, it, 256) } },
)

// ---- Local (Room) entity → domain, used by the offline search path ----

fun ArtistEntity.toModel(serverUrl: String?): Artist = Artist(
    id = id,
    name = name,
    coverArtUrl = coverArt?.let { serverUrl?.let { s -> CoverArtUrl.of(s, it, 256) } },
)

fun SongEntity.toModel(serverUrl: String?): Song = Song(
    id = id,
    title = title,
    artistName = artistName,
    albumName = null, // not stored on the song row
    albumId = albumId,
    duration = duration,
    coverArtUrl = coverArt?.let { serverUrl?.let { s -> CoverArtUrl.of(s, it, 256) } },
    downloadState = downloadState,
    localPath = localPath,
)
