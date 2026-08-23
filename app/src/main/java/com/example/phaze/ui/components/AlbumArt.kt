package com.example.phaze.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import coil.compose.SubcomposeAsyncImage

/**
 * A square album cover rendering the server-provided artwork through Coil
 * (the authenticated ImageLoader supplied at the app root).
 *
 * A neutral placeholder is shown only while the image loads or if the album
 * has no art on the server — album covers always come from the server.
 */
@Composable
fun AlbumArt(
    coverUrl: String?,
    name: String,
    size: Dp = Dp.Unspecified,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(0.dp)
    val base = if (size != Dp.Unspecified) {
        modifier.size(size).clip(shape)
    } else {
        modifier.clip(shape)
    }

    if (coverUrl.isNullOrBlank()) {
        Log.v(TAG, "AlbumArt: no cover art for '$name' — showing placeholder")
        ArtPlaceholder(base)
        return
    }

    SubcomposeAsyncImage(
        model = coverUrl,
        contentDescription = name,
        contentScale = ContentScale.Crop,
        modifier = base.clipToBounds(),
        loading = { ArtPlaceholder(Modifier.fillMaxSize()) },
        error = {
            LaunchedEffect(coverUrl) {
                Log.w(TAG, "AlbumArt: failed to load cover for '$name' (url=$coverUrl)")
            }
            ArtPlaceholder(Modifier.fillMaxSize())
        },
    )
}

private const val TAG = "AlbumArt"

@Composable
private fun ArtPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp),
        )
    }
}
