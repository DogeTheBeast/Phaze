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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.phaze2.ui.components.BottomNav
import com.example.phaze2.ui.components.MiniPlayer
import com.example.phaze2.ui.navigation.Phaze2NavHost
import com.example.phaze2.ui.navigation.Routes
import com.example.phaze2.ui.theme.Phaze2Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Phaze2Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Phaze2App()
                }
            }
        }
    }
}

@Composable
fun Phaze2App() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

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
            modifier = Modifier.padding(innerPadding)
        )
    }
}
