package com.example.phaze.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.phaze.ui.screens.album.AlbumScreen
import com.example.phaze.ui.screens.artist.ArtistScreen
import com.example.phaze.ui.screens.downloads.DownloadsScreen
import com.example.phaze.ui.screens.filter.FilterScreen
import com.example.phaze.ui.screens.home.HomeScreen
import com.example.phaze.ui.screens.library.LibraryScreen
import com.example.phaze.ui.screens.player.PlayerScreen
import com.example.phaze.ui.screens.queue.QueueScreen
import com.example.phaze.ui.screens.search.SearchScreen
import com.example.phaze.ui.screens.settings.SettingsScreen
import com.example.phaze.ui.screens.setup.SetupScreen

@Composable
fun PhazeNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Routes.SETUP) { SetupScreen(navController) }
        composable(Routes.HOME) { HomeScreen(navController) }
        composable(Routes.LIBRARY) { LibraryScreen(navController) }
        composable(Routes.ARTIST) { ArtistScreen(navController) }
        composable(Routes.ALBUM) { AlbumScreen(navController) }
        composable(Routes.FILTER) { FilterScreen(navController) }
        composable(Routes.PLAYER) { PlayerScreen(navController) }
        composable(Routes.QUEUE) { QueueScreen(navController) }
        composable(Routes.DOWNLOADS) { DownloadsScreen(navController) }
        composable(Routes.SEARCH) { SearchScreen(navController) }
        composable(Routes.SETTINGS) { SettingsScreen(navController) }
    }
}
