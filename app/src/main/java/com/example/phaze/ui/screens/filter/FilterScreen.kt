package com.example.phaze.ui.screens.filter

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.phaze.data.model.Album
import com.example.phaze.data.model.Artist
import com.example.phaze.data.model.Playlist
import com.example.phaze.data.model.Song
import com.example.phaze.ui.components.AlbumArt
import java.util.Locale

@Composable
fun FilterScreen(
    navController: NavHostController,
    viewModel: FilterViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        FilterTopBar(
            title = state.type.title,
            count = state.headingCount(),
            onBack = { navController.popBackStack() },
        )

        when {
            state.isLoading && state.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.type.kind == FilterType.Kind.EMPTY -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("${state.type.title} isn't available yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> when (state.type.kind) {
                FilterType.Kind.ALBUMS -> AlbumContent(state.albums, onAlbumClick = { navController.navigate("album/${it.id}") })
                FilterType.Kind.ARTISTS -> ArtistContent(state.artists, onArtistClick = { navController.navigate("artist/${it.id}") })
                FilterType.Kind.SONGS -> SongContent(state.songs, onSongClick = { albumId -> albumId?.let { navController.navigate("album/$it") } })
                FilterType.Kind.PLAYLISTS -> PlaylistContent(state.playlists)
                FilterType.Kind.EMPTY -> {}
            }
        }
    }
}

@Composable
private fun FilterTopBar(title: String, count: Int, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        if (count > 0) {
            Text("$count", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---- Albums (with grid/list toggle) ----

@Composable
private fun AlbumContent(albums: List<Album>, onAlbumClick: (Album) -> Unit) {
    var listView by rememberSaveable { mutableStateOf(false) }
    if (albums.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No albums yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(onClick = { listView = !listView }) {
                Icon(
                    imageVector = if (listView) Icons.Filled.ViewModule else Icons.Filled.ViewAgenda,
                    contentDescription = "Toggle view",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (listView) {
            AlbumList(albums, onAlbumClick)
        } else {
            AlbumGrid(albums, onAlbumClick)
        }
    }
}

@Composable
private fun AlbumGrid(albums: List<Album>, onAlbumClick: (Album) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
        items(albums.chunked(2), key = { it.first().id }) { rowAlbums ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                rowAlbums.forEach { album -> AlbumGridCard(album, Modifier.weight(1f), onClick = { onAlbumClick(album) }) }
                if (rowAlbums.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AlbumGridCard(album: Album, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier = modifier.clickable(onClick = onClick)) {
        AlbumArt(coverUrl = album.coverArtUrl, name = album.name, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
        Spacer(Modifier.height(6.dp))
        Text(album.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(album.artistName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AlbumList(albums: List<Album>, onAlbumClick: (Album) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
        items(albums, key = { it.id }) { album ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = { onAlbumClick(album) }).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumArt(coverUrl = album.coverArtUrl, name = album.name, size = 48.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(album.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(album.artistName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ---- Artists / Songs / Playlists ----

@Composable
private fun ArtistContent(artists: List<Artist>, onArtistClick: (Artist) -> Unit) {
    if (artists.isEmpty()) {
        Empty("No artists yet"); return
    }
    LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
        items(artists, key = { it.id }) { artist ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = { onArtistClick(artist) }).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumArt(coverUrl = artist.coverArtUrl, name = artist.name, size = 48.dp)
                Spacer(Modifier.width(12.dp))
                Text(artist.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun SongContent(songs: List<Song>, onSongClick: (String?) -> Unit) {
    if (songs.isEmpty()) {
        Empty("No songs yet"); return
    }
    LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
        items(songs, key = { it.id }) { song ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = { onSongClick(song.albumId) }).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumArt(coverUrl = song.coverArtUrl, name = song.title, size = 40.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(song.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artistName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (song.duration > 0) {
                    Text(formatDuration(song.duration), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun PlaylistContent(playlists: List<Playlist>) {
    if (playlists.isEmpty()) {
        Empty("No playlists yet"); return
    }
    LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
        items(playlists, key = { it.id }) { playlist ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(playlist.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (playlist.songCount > 0) {
                    Text("${playlist.songCount} songs · ${(playlist.duration + 59) / 60} min", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun Empty(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun FilterUiState.headingCount(): Int = when (type.kind) {
    FilterType.Kind.ALBUMS -> albums.size
    FilterType.Kind.ARTISTS -> artists.size
    FilterType.Kind.SONGS -> songs.size
    FilterType.Kind.PLAYLISTS -> playlists.size
    FilterType.Kind.EMPTY -> 0
}

private fun FilterUiState.isEmpty(): Boolean = albums.isEmpty() && artists.isEmpty() && songs.isEmpty() && playlists.isEmpty()

private fun formatDuration(seconds: Int): String =
    String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60)
