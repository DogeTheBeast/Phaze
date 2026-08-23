package com.example.phaze.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.phaze.data.local.entity.ServerEntity
import com.example.phaze.data.repository.SettingsPreferences
import com.example.phaze.ui.navigation.Routes
import com.example.phaze.ui.theme.Accent
import com.example.phaze.ui.theme.Success
import java.util.Locale
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val server by viewModel.server.collectAsStateWithLifecycle()
    val connection by viewModel.connectionState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
    ) {
        item(key = "title") {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )
        }

        item(key = "server-label") { SectionLabel("Server") }

        item(key = "server-row") {
            if (server == null) {
                NavigableRow(
                    icon = Icons.Filled.Dns,
                    title = "Add server",
                    subtitle = "Not connected",
                    onClick = { navController.navigate(Routes.SETUP) },
                )
            } else {
                ServerRow(server = server!!, onClick = { navController.navigate(Routes.SETUP) })
            }
        }

        item(key = "test-row") {
            TestConnectionRow(state = connection, onClick = { viewModel.testConnection() })
        }

        item(key = "playback-label") { SectionLabel("Playback") }
        item(key = "stream-wifi") { ValueRow("Streaming quality (Wi-Fi)", settings.streamQualityWifi) }
        item(key = "stream-mobile") { ValueRow("Streaming quality (mobile data)", settings.streamQualityMobile) }
        item(key = "gapless") {
            SwitchRow("Gapless playback", checked = settings.gaplessPlayback, onCheckedChange = viewModel::setGaplessPlayback)
        }

        item(key = "downloads-label") { SectionLabel("Downloads") }
        item(key = "download-quality") { ValueRow("Download quality", settings.downloadQuality) }
        item(key = "wifi-only") {
            SwitchRow("Download on Wi-Fi only", checked = settings.downloadOnWifiOnly, onCheckedChange = viewModel::setDownloadOnWifiOnly)
        }
        item(key = "auto-starred") {
            SwitchRow(
                "Auto-download starred songs",
                subtitle = "New starred songs download on Wi-Fi",
                checked = settings.autoDownloadStarred,
                onCheckedChange = viewModel::setAutoDownloadStarred,
            )
        }
        item(key = "storage-limit") { ValueRow("Storage limit", formatBytes(settings.storageLimitBytes)) }

        item(key = "offline-label") { SectionLabel("Offline") }
        item(key = "offline-mode") {
            SwitchRow(
                "Offline mode",
                subtitle = "Only show downloaded music",
                checked = settings.offlineMode,
                onCheckedChange = viewModel::setOfflineMode,
            )
        }
        item(key = "manage-downloads") {
            NavigableRow("Manage downloads", onClick = { navController.navigate(Routes.DOWNLOADS) }, trailingChevron = true)
        }

        item(key = "library-label") { SectionLabel("Library") }
        item(key = "scrobble") {
            SwitchRow("Scrobble plays to server", checked = settings.scrobble, onCheckedChange = viewModel::setScrobble)
        }
        item(key = "offline-banner") {
            SwitchRow("Show offline banner", checked = settings.showOfflineBanner, onCheckedChange = viewModel::setShowOfflineBanner)
        }

        item(key = "appearance-label") { SectionLabel("Appearance") }
        item(key = "theme") { ValueRow("Theme", "Dark") }
        item(key = "accent") { AccentRow(selectedKey = settings.accentKey, onSelect = viewModel::setAccent) }

        item(key = "about-label") { SectionLabel("About") }
        item(key = "about") {
            Column(Modifier.padding(vertical = 12.dp)) {
                Text("Phaze", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text("Version 0.1.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun RowScaffold(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing?.invoke()
    }
}

@Composable
private fun ServerRow(server: ServerEntity, onClick: () -> Unit) {
    val host = server.url.toHttpUrlOrNull()?.host ?: server.url
    NavigableRow(
        icon = Icons.Filled.Dns,
        title = "Home server",
        subtitle = "$server.username@$host · ${server.serverType ?: "Subsonic"}",
        onClick = onClick,
        trailingChevron = true,
    )
}

@Composable
private fun NavigableRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    trailingChevron: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailingChevron) {
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TestConnectionRow(state: ConnectionState, onClick: () -> Unit) {
    RowScaffold(
        title = "Test connection",
        modifier = Modifier.clickable(enabled = state !is ConnectionState.Testing, onClick = onClick),
        trailing = {
            when (state) {
                ConnectionState.Idle -> {}
                ConnectionState.Testing -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                is ConnectionState.Ok -> Text("OK", style = MaterialTheme.typography.bodyMedium, color = Success)
                is ConnectionState.Error -> Text(state.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        },
    )
}

@Composable
private fun ValueRow(title: String, value: String) {
    RowScaffold(
        title = title,
        trailing = {
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
    )
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
) {
    RowScaffold(
        title = title,
        subtitle = subtitle,
        trailing = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
    )
}

@Composable
private fun AccentRow(selectedKey: String, onSelect: (String) -> Unit) {
    RowScaffold(
        title = "Accent color",
        trailing = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Accent.entries.forEach { accent ->
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(accent.primary)
                            .border(
                                width = 2.dp,
                                color = if (accent.key == selectedKey) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    Color.Transparent
                                },
                                shape = CircleShape,
                            )
                            .clickable { onSelect(accent.key) },
                    )
                }
            }
        },
    )
}

private fun formatBytes(bytes: Long): String {
    val gb = bytes.toDouble() / (1024.0 * 1024 * 1024)
    val mb = bytes / (1024 * 1024)
    return when {
        gb >= 1.0 -> if (gb == gb.toLong().toDouble()) "${gb.toLong()} GB" else String.format(Locale.US, "%.1f GB", gb)
        else -> "$mb MB"
    }
}
