package com.example.phaze2.data.mapper

import com.example.phaze2.data.local.entity.AlbumEntity
import com.example.phaze2.data.model.Album
import com.example.phaze2.data.remote.CoverArtUrl

/**
 * Maps a stored [AlbumEntity] to the UI [Album] view, resolving the cover-art
 * URL against the active [serverUrl] (null when no server is configured).
 */
fun AlbumEntity.toModel(serverUrl: String?): Album = Album(
    id = id,
    name = name,
    artistName = artistName,
    coverArtUrl = coverArt?.let { artId ->
        serverUrl?.let { CoverArtUrl.of(it, artId, CoverArtUrl.RAIL) }
    },
)
