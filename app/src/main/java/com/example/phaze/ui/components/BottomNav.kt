package com.example.phaze.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.phaze.ui.navigation.Routes

private val navItems = listOf(
    Routes.HOME to Icons.Default.Home,
    Routes.SEARCH to Icons.Default.Search,
    Routes.DOWNLOADS to Icons.Outlined.Download,
    Routes.SETTINGS to Icons.Default.Settings
)

@Composable
fun BottomNav(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier.height(72.dp)) {
        navItems.forEach { (route, icon) ->
            val selected = currentRoute == route
            NavigationBarItem(
                icon = { Icon(imageVector = icon, contentDescription = route) },
                selected = selected,
                onClick = { onNavigate(route) },
                label = null,
                alwaysShowLabel = false
            )
        }
    }
}
