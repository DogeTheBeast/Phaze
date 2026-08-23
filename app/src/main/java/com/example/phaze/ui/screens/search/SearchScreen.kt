package com.example.phaze.ui.screens.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.phaze.data.model.Album
import com.example.phaze.data.model.Artist
import com.example.phaze.data.model.SearchResults
import com.example.phaze.data.model.Song
import com.example.phaze.ui.components.AlbumArt
import com.example.phaze.ui.navigation.Routes
import java.util.Locale

private data class BrowseItem(val label: String, val type: String, val icon: ImageVector)

@Composable
fun SearchScreen(
    navController: NavHostController,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current

    // Back gesture with an active search: clear the bar and collapse the
    // keyboard first; a second back (when empty) performs normal navigation.
    BackHandler(enabled = uiState.query.isNotEmpty()) {
        viewModel.clearQuery()
        keyboard?.hide()
    }

    val openArtist: (Artist) -> Unit = { navController.navigate(Routes.artist(it.id)) }
    val openAlbum: (Album) -> Unit = { navController.navigate(Routes.album(it.id)) }
    val openSong: (Song) -> Unit = { song ->
        song.albumId?.let { navController.navigate(Routes.album(it)) }
    }

    Column(Modifier.fillMaxSize()) {
        SearchBar(
            query = uiState.query,
            onQueryChange = viewModel::onQueryChange,
            onSearch = {
                viewModel.onSearchSubmit()
                keyboard?.hide()
            },
            onClear = {
                viewModel.clearQuery()
                keyboard?.hide()
            },
        )

        when (val mode = uiState.mode) {
            SearchMode.Idle -> IdleContent(
                recentSearches = uiState.recentSearches,
                onRecentClick = { viewModel.onRecentClick(it) },
                onBrowseClick = { item -> navController.navigate(Routes.filter(item.type)) },
            )
            SearchMode.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is SearchMode.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = mode.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            is SearchMode.Results -> {
                if (mode.results.isEmpty) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No results",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    ResultsContent(
                        results = mode.results,
                        onArtistClick = openArtist,
                        onAlbumClick = openAlbum,
                        onSongClick = openSong,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        placeholder = { Text("Search songs, albums, artists") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdleContent(
    recentSearches: List<String>,
    onRecentClick: (String) -> Unit,
    onBrowseClick: (BrowseItem) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
    ) {
        if (recentSearches.isNotEmpty()) {
            item(key = "recent-header") {
                SectionHeader("Recent searches", Modifier.padding(top = 4.dp, bottom = 8.dp))
            }
            item(key = "recent-chips") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    recentSearches.forEach { term ->
                        SuggestionChip(
                            onClick = { onRecentClick(term) },
                            label = { Text(term) },
                        )
                    }
                }
            }
        }
        item(key = "browse-header") {
            SectionHeader("Browse", Modifier.padding(top = 22.dp, bottom = 8.dp))
        }
        item(key = "browse-grid") {
            BrowseGrid(onBrowseClick)
        }
    }
}

@Composable
private fun BrowseGrid(onBrowseClick: (BrowseItem) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        browseItems().chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowItems.forEach { item ->
                    BrowseTile(item, onClick = { onBrowseClick(item) }, modifier = Modifier.weight(1f))
                }
                // Keep the last incomplete row's tiles the same size (mockup .searchgrid).
                repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun BrowseTile(
    item: BrowseItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.aspectRatio(1f),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ResultsContent(
    results: SearchResults,
    onArtistClick: (Artist) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onSongClick: (Song) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
    ) {
        if (results.artists.isNotEmpty()) {
            item(key = "artists-header") { SectionHeader("Artists", Modifier.padding(top = 4.dp, bottom = 4.dp)) }
            items(results.artists, key = { "a-${it.id}" }) { artist ->
                ArtistRow(artist, onClick = { onArtistClick(artist) })
            }
        }
        if (results.albums.isNotEmpty()) {
            item(key = "albums-header") { SectionHeader("Albums", Modifier.padding(top = 18.dp, bottom = 4.dp)) }
            items(results.albums, key = { "al-${it.id}" }) { album ->
                AlbumRow(album, onClick = { onAlbumClick(album) })
            }
        }
        if (results.songs.isNotEmpty()) {
            item(key = "songs-header") { SectionHeader("Songs", Modifier.padding(top = 18.dp, bottom = 4.dp)) }
            items(results.songs, key = { "s-${it.id}" }) { song ->
                SongRow(song, onClick = { onSongClick(song) })
            }
        }
    }
}

@Composable
private fun ArtistRow(artist: Artist, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(coverUrl = artist.coverArtUrl, name = artist.name, size = 48.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AlbumRow(album: Album, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(coverUrl = album.coverArtUrl, name = album.name, size = 48.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = album.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = album.artistName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SongRow(song: Song, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(coverUrl = song.coverArtUrl, name = song.title, size = 48.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = listOfNotNull(song.artistName, song.albumName).joinToString(" · ")
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (song.duration > 0) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = formatDuration(song.duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier,
    )
}

private fun formatDuration(seconds: Int): String =
    String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60)

private fun browseItems(): List<BrowseItem> = listOf(
    BrowseItem("Albums", "albums", Icons.Filled.Album),
    BrowseItem("Artists", "artists", Icons.Filled.Person),
    BrowseItem("Genres", "genres", Icons.Filled.Category),
    BrowseItem("Songs", "songs", Icons.Filled.QueueMusic),
    BrowseItem("Playlists", "playlists", Icons.Filled.PlaylistPlay),
    BrowseItem("Years", "years", Icons.Filled.DateRange),
    BrowseItem("Moods", "moods", Icons.Filled.Mood),
)
