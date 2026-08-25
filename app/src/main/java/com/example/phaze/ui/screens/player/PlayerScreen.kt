package com.example.phaze.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.phaze.ui.components.AlbumArt
import com.example.phaze.ui.navigation.Routes
import com.example.phaze.ui.theme.Success
import java.util.Locale

@Composable
fun PlayerScreen(
    navController: NavHostController,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        PlayerTopBar(
            album = state.album,
            onBack = { navController.popBackStack() },
            onMore = { /* TODO */ },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            AlbumArt(
                coverUrl = state.coverArtUrl,
                name = state.title,
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .aspectRatio(1f),
            )

            Spacer(Modifier.height(24.dp))
            Spacer(Modifier.weight(1f))

            Text(
                text = state.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clickable(enabled = state.albumId != null) {
                        state.albumId?.let { navController.navigate(Routes.album(it)) }
                    },
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = state.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clickable(enabled = state.artistId != null) {
                        state.artistId?.let { navController.navigate(Routes.artist(it)) }
                    },
            )

            Spacer(Modifier.height(24.dp))
            SeekBar(
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                onSeek = viewModel::seekTo,
            )

            Spacer(Modifier.height(12.dp))
            TransportControls(
                isPlaying = state.isPlaying,
                isShuffle = state.isShuffle,
                isRepeat = state.isRepeat,
                onPlayPause = viewModel::playPause,
                onShuffle = viewModel::toggleShuffle,
                onRepeat = viewModel::toggleRepeat,
                onPrevious = viewModel::previous,
                onNext = viewModel::next,
            )

            Spacer(Modifier.height(28.dp))
            PlayerFooter(
                quality = state.quality,
                onUpNext = { navController.navigate(Routes.QUEUE) },
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PlayerTopBar(album: String, onBack: () -> Unit, onMore: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(
            text = "Playing from album · $album",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onMore) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More")
        }
    }
}

@Composable
private fun SeekBar(positionMs: Long, durationMs: Long, onSeek: (Long) -> Unit) {
    val positionSec = (positionMs / 1000).toInt()
    val durationSec = (durationMs / 1000).toInt()
    Column(Modifier.fillMaxWidth()) {
        Slider(
            value = positionSec.toFloat(),
            onValueChange = { onSeek((it * 1000).toLong()) },
            valueRange = 0f..durationSec.toFloat().coerceAtLeast(1f),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(positionSec), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatDuration(durationSec), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TransportControls(
    isPlaying: Boolean,
    isShuffle: Boolean,
    isRepeat: Boolean,
    onPlayPause: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        IconButton(onClick = onShuffle) {
            Icon(
                imageVector = Icons.Filled.Shuffle,
                contentDescription = "Shuffle",
                tint = if (isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onPrevious, modifier = Modifier.size(44.dp)) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(32.dp))
        }
        // Play / pause FAB
        Box(
            modifier = Modifier
                .size(64.dp)
                .clickable(onClick = onPlayPause)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(32.dp),
            )
        }
        IconButton(onClick = onNext, modifier = Modifier.size(44.dp)) {
            Icon(Icons.Filled.SkipNext, contentDescription = "Next", modifier = Modifier.size(32.dp))
        }
        IconButton(onClick = onRepeat) {
            Icon(
                imageVector = Icons.Filled.Repeat,
                contentDescription = "Repeat",
                tint = if (isRepeat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlayerFooter(quality: String, onUpNext: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Quality chip
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Success, modifier = Modifier.size(14.dp))
                Text(quality, style = MaterialTheme.typography.labelMedium)
            }
        }
        IconButton(onClick = { /* TODO: lyrics */ }) {
            Icon(Icons.Filled.Article, contentDescription = "Lyrics", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = { /* TODO: cast */ }) {
            Icon(Icons.Filled.Cast, contentDescription = "Cast", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onUpNext) {
            Icon(Icons.Filled.QueueMusic, contentDescription = "Up next", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun formatDuration(seconds: Int): String =
    String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60)
