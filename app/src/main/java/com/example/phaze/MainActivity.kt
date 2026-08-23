package com.example.phaze

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.ImageLoader
import coil.compose.LocalImageLoader
import com.example.phaze.ui.MainViewModel
import com.example.phaze.ui.components.BottomNav
import com.example.phaze.ui.components.MiniPlayer
import com.example.phaze.ui.navigation.PhazeNavHost
import com.example.phaze.ui.navigation.Routes
import com.example.phaze.ui.theme.PhazeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val accent by mainViewModel.accent.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalImageLoader provides imageLoader) {
                PhazeTheme(accent = accent) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        PhazeApp(mainViewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun PhazeApp(mainViewModel: MainViewModel) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val startDestination by mainViewModel.startDestination.collectAsStateWithLifecycle()

    // Wait for the DB read so we don't flash the wrong start screen.
    val start = startDestination ?: return
    val playbackState by mainViewModel.playbackState.collectAsStateWithLifecycle()

    val bottomNavRoutes = setOf(Routes.HOME, Routes.SEARCH, Routes.DOWNLOADS, Routes.SETTINGS)
    val miniPlayerRoutes = setOf(
        Routes.HOME, Routes.SEARCH, Routes.LIBRARY, Routes.DOWNLOADS, Routes.SETTINGS,
        Routes.ARTIST, Routes.ALBUM, Routes.FILTER,
    )
    val showBottomNav = currentRoute in bottomNavRoutes
    val showMiniPlayer = currentRoute in miniPlayerRoutes && playbackState.hasCurrent

    Scaffold(
        bottomBar = {
            if (showBottomNav || showMiniPlayer) {
                Column {
                    if (showMiniPlayer) {
                        MiniPlayer(
                            state = playbackState,
                            onOpenPlayer = { navController.navigate(Routes.PLAYER) },
                            onPlayPause = mainViewModel::playPause,
                        )
                    }
                    if (showBottomNav) {
                        BottomNav(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                if (route != currentRoute) {
                                    navController.navigate(route) {
                                        popUpTo(Routes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        PhazeNavHost(
            navController = navController,
            startDestination = start,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
