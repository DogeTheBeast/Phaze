package com.example.phaze.ui.screens.artist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.phaze.data.model.Album
import com.example.phaze.data.model.ArtistDetail
import com.example.phaze.ui.components.AlbumArt
import com.example.phaze.ui.navigation.Routes
import com.example.phaze.ui.theme.GradientDirection
import com.example.phaze.ui.theme.screenBackgroundBrush
import com.example.phaze.ui.theme.Success

@Composable
fun ArtistScreen(
    navController: NavHostController,
    viewModel: ArtistViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBackgroundBrush(GradientDirection.TOP_BOTTOM)),
    ) {
        ArtistTopBar(onBack = { navController.popBackStack() })

        when {
            state.isLoading && state.detail.isEmpty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.errorMessage != null && state.detail.isEmpty -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(state.errorMessage.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
            else -> ArtistContent(
                detail = state.detail,
                onPlay = { navController.navigate(Routes.PLAYER) },
                onShuffle = { navController.navigate(Routes.PLAYER) },
                onStarToggle = viewModel::toggleStar,
                onAlbumClick = { album -> navController.navigate(Routes.album(album.id)) },
            )
        }
    }
}

@Composable
private fun ArtistTopBar(onBack: () -> Unit) {
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
        IconButton(onClick = { /* TODO: overflow menu */ }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More")
        }
    }
}

@Composable
private fun ArtistContent(
    detail: ArtistDetail,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onStarToggle: () -> Unit,
    onAlbumClick: (Album) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "hero") { ArtistHero(detail) }
        item(key = "actions") { ArtistActions(detail, onPlay, onShuffle, onStarToggle) }
        item(key = "albums-header") {
            Text(
                text = "Albums",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
        item(key = "albums-grid") {
            AlbumsGrid(detail.albums, onAlbumClick)
        }
    }
}

@Composable
private fun ArtistHero(detail: ArtistDetail) {
    Column(
        modifier = Modifier.padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(96.dp).clip(CircleShape)) {
            AlbumArt(coverUrl = detail.coverArtUrl, name = detail.name, size = 96.dp)
        }
        Spacer(Modifier.height(10.dp))
        Text(detail.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        val meta = listOfNotNull(
            if (detail.albumCount > 0) "${detail.albumCount} ${if (detail.albumCount == 1) "album" else "albums"}" else null,
            if (detail.songCount > 0) "${detail.songCount} songs" else null,
        ).joinToString(" · ")
        if (meta.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ArtistActions(
    detail: ArtistDetail,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onStarToggle: () -> Unit,
) {
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(onClick = onPlay, contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp), shape = CircleShape) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text("Play")
        }
        FilledTonalButton(onClick = onShuffle, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp), shape = CircleShape) {
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
private fun AlbumsGrid(albums: List<Album>, onAlbumClick: (Album) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        albums.chunked(2).forEach { rowAlbums ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                rowAlbums.forEach { album ->
                    AlbumCard(album, onClick = { onAlbumClick(album) }, modifier = Modifier.weight(1f))
                }
                if (rowAlbums.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AlbumCard(album: Album, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
    ) {
        AlbumArt(
            coverUrl = album.coverArtUrl,
            name = album.name,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        )
        Spacer(Modifier.height(6.dp))
        Text(album.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        album.year?.let { year ->
            Text(year.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
