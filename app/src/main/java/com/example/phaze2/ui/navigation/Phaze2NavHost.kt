package com.example.phaze2.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.phaze2.ui.screens.album.AlbumScreen
import com.example.phaze2.ui.screens.artist.ArtistScreen
import com.example.phaze2.ui.screens.downloads.DownloadsScreen
import com.example.phaze2.ui.screens.home.HomeScreen
import com.example.phaze2.ui.screens.library.LibraryScreen
import com.example.phaze2.ui.screens.player.PlayerScreen
import com.example.phaze2.ui.screens.queue.QueueScreen
import com.example.phaze2.ui.screens.search.SearchScreen
import com.example.phaze2.ui.screens.settings.SettingsScreen
import com.example.phaze2.ui.screens.setup.SetupScreen

@Composable
fun Phaze2NavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier
    ) {
        composable(Routes.SETUP) { SetupScreen(navController) }
        composable(Routes.HOME) { HomeScreen(navController) }
        composable(Routes.LIBRARY) { LibraryScreen(navController) }
        composable(Routes.ARTIST) { ArtistScreen(navController) }
        composable(Routes.ALBUM) { AlbumScreen(navController) }
        composable(Routes.PLAYER) { PlayerScreen(navController) }
        composable(Routes.QUEUE) { QueueScreen(navController) }
        composable(Routes.DOWNLOADS) { DownloadsScreen(navController) }
        composable(Routes.SEARCH) { SearchScreen(navController) }
        composable(Routes.SETTINGS) { SettingsScreen(navController) }
    }
}
