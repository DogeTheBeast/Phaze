package com.example.phaze2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.example.phaze2.ui.MainViewModel
import com.example.phaze2.ui.components.BottomNav
import com.example.phaze2.ui.components.MiniPlayer
import com.example.phaze2.ui.navigation.Phaze2NavHost
import com.example.phaze2.ui.navigation.Routes
import com.example.phaze2.ui.theme.Phaze2Theme
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
            CompositionLocalProvider(LocalImageLoader provides imageLoader) {
                Phaze2Theme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Phaze2App()
                    }
                }
            }
        }
    }
}

@Composable
fun Phaze2App(mainViewModel: MainViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val startDestination by mainViewModel.startDestination.collectAsStateWithLifecycle()

    // Wait for the DB read so we don't flash the wrong start screen.
    val start = startDestination ?: return

    val showBars = currentRoute in listOf(
        Routes.HOME,
        Routes.LIBRARY,
        Routes.DOWNLOADS,
        Routes.SETTINGS
    )

    Scaffold(
        bottomBar = {
            if (showBars) {
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
    ) { innerPadding ->
        Phaze2NavHost(
            navController = navController,
            startDestination = start,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
