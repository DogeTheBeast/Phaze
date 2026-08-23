package com.example.phaze.ui.screens.album

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.phaze.data.model.AlbumDetail
import com.example.phaze.data.model.DownloadState
import com.example.phaze.data.model.Song
import com.example.phaze.ui.components.AlbumArt
import com.example.phaze.ui.navigation.Routes
import com.example.phaze.ui.theme.GradientDirection
import com.example.phaze.ui.theme.screenBackgroundBrush
import com.example.phaze.ui.theme.Success
import java.util.Locale

@Composable
fun AlbumScreen(
    navController: NavHostController,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBackgroundBrush(GradientDirection.BOTTOM_RIGHT)),
    ) {
        AlbumTopBar(
            onBack = { navController.popBackStack() },
            onMore = { /* TODO: overflow menu */ },
        )

        when {
            state.isLoading && state.detail.isEmpty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.errorMessage != null && state.detail.isEmpty -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    state.errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            else -> AlbumContent(
                detail = state.detail,
                onPlay = { viewModel.playAt(0) },
                onShuffle = { viewModel.playAt(0) },
                onStarToggle = viewModel::toggleStar,
                onArtistClick = { navController.navigate(Routes.artist(state.detail.artistId)) },
                onTrackClick = viewModel::playAt,
            )
        }
    }
}

@Composable
private fun AlbumTopBar(onBack: () -> Unit, onMore: () -> Unit) {
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
        IconButton(onClick = onMore) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More")
        }
    }
}

@Composable
private fun AlbumContent(
    detail: AlbumDetail,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onStarToggle: () -> Unit,
    onArtistClick: () -> Unit,
    onTrackClick: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "hero") { AlbumHero(detail, onArtistClick) }
        item(key = "actions") { AlbumActions(detail, onPlay, onShuffle, onStarToggle) }
        item(key = "tracks-header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Track list", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${detail.songs.size} tracks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        itemsIndexed(detail.songs, key = { _, song -> song.id }) { index, song ->
            TrackRow(
                index = index,
                song = song,
                onClick = { onTrackClick(index) },
            )
        }
    }
}

@Composable
private fun AlbumHero(detail: AlbumDetail, onArtistClick: () -> Unit) {
    Column(
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AlbumArt(coverUrl = detail.coverArtUrl, name = detail.name, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Text(
            text = detail.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = detail.artistName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onArtistClick),
        )
        val meta = albumMeta(detail)
        if (meta.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AlbumActions(
    detail: AlbumDetail,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onStarToggle: () -> Unit,
) {
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onPlay,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
            shape = CircleShape,
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text("Play")
        }
        FilledTonalButton(
            onClick = onShuffle,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            shape = CircleShape,
        ) {
            Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text("Shuffle")
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onStarToggle) {
            Icon(
                imageVector = if (detail.starred) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = "Star",
                tint = if (detail.starred) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TrackRow(index: Int, song: Song, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = (index + 1).toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(24.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (song.duration > 0) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = formatDuration(song.duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        DownloadBadge(song.downloadState)
    }
}

@Composable
private fun DownloadBadge(state: DownloadState) {
    when (state) {
        DownloadState.DOWNLOADED -> Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = "Downloaded",
            tint = Success,
            modifier = Modifier.size(18.dp),
        )
        DownloadState.IN_PROGRESS -> Icon(
            imageVector = Icons.Filled.Download,
            contentDescription = "Downloading",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        DownloadState.NONE -> Spacer(Modifier.size(18.dp))
    }
}

private fun albumMeta(d: AlbumDetail): String {
    val parts = mutableListOf<String>()
    d.year?.let { parts += it.toString() }
    if (d.songCount > 0) parts += "${d.songCount} ${if (d.songCount == 1) "song" else "songs"}"
    if (d.duration > 0) parts += "${(d.duration + 59) / 60} min"
    return parts.joinToString(" · ")
}

private fun formatDuration(seconds: Int): String =
    String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60)
