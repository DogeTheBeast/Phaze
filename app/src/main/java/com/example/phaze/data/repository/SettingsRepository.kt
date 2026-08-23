package com.example.phaze.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * User preferences persisted in Preferences DataStore (PLAN.md §8, settings.html).
 * Exposed as an immutable snapshot [SettingsPreferences] via [settings].
 */
data class SettingsPreferences(
    val gaplessPlayback: Boolean = true,
    val streamQualityWifi: String = "Max (FLAC)",
    val streamQualityMobile: String = "192 kbps",
    val downloadQuality: String = "320 kbps",
    val downloadOnWifiOnly: Boolean = true,
    val autoDownloadStarred: Boolean = false,
    val storageLimitBytes: Long = DEFAULT_STORAGE_LIMIT_BYTES,
    val offlineMode: Boolean = false,
    val scrobble: Boolean = true,
    val showOfflineBanner: Boolean = false,
    val accentKey: String = "blue",
) {
    companion object {
        const val DEFAULT_STORAGE_LIMIT_BYTES = 8L * 1024 * 1024 * 1024
    }
}

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.settingsDataStore

    private object Keys {
        val gapless = booleanPreferencesKey("gapless_playback")
        val streamQualityWifi = stringPreferencesKey("stream_quality_wifi")
        val streamQualityMobile = stringPreferencesKey("stream_quality_mobile")
        val downloadQuality = stringPreferencesKey("download_quality")
        val downloadOnWifiOnly = booleanPreferencesKey("download_wifi_only")
        val autoDownloadStarred = booleanPreferencesKey("auto_download_starred")
        val storageLimit = longPreferencesKey("storage_limit_bytes")
        val offlineMode = booleanPreferencesKey("offline_mode")
        val scrobble = booleanPreferencesKey("scrobble")
        val showOfflineBanner = booleanPreferencesKey("show_offline_banner")
        val accentKey = stringPreferencesKey("accent_key")
    }

    val settings: Flow<SettingsPreferences> = dataStore.data.map { prefs ->
        SettingsPreferences(
            gaplessPlayback = prefs[Keys.gapless] ?: true,
            streamQualityWifi = prefs[Keys.streamQualityWifi] ?: "Max (FLAC)",
            streamQualityMobile = prefs[Keys.streamQualityMobile] ?: "192 kbps",
            downloadQuality = prefs[Keys.downloadQuality] ?: "320 kbps",
            downloadOnWifiOnly = prefs[Keys.downloadOnWifiOnly] ?: true,
            autoDownloadStarred = prefs[Keys.autoDownloadStarred] ?: false,
            storageLimitBytes = prefs[Keys.storageLimit] ?: SettingsPreferences.DEFAULT_STORAGE_LIMIT_BYTES,
            offlineMode = prefs[Keys.offlineMode] ?: false,
            scrobble = prefs[Keys.scrobble] ?: true,
            showOfflineBanner = prefs[Keys.showOfflineBanner] ?: false,
            accentKey = prefs[Keys.accentKey] ?: "blue",
        )
    }

    suspend fun setGaplessPlayback(enabled: Boolean) = dataStore.edit { it[Keys.gapless] = enabled }
    suspend fun setStreamQualityWifi(value: String) = dataStore.edit { it[Keys.streamQualityWifi] = value }
    suspend fun setStreamQualityMobile(value: String) = dataStore.edit { it[Keys.streamQualityMobile] = value }
    suspend fun setDownloadQuality(value: String) = dataStore.edit { it[Keys.downloadQuality] = value }
    suspend fun setDownloadOnWifiOnly(enabled: Boolean) = dataStore.edit { it[Keys.downloadOnWifiOnly] = enabled }
    suspend fun setAutoDownloadStarred(enabled: Boolean) = dataStore.edit { it[Keys.autoDownloadStarred] = enabled }
    suspend fun setStorageLimitBytes(bytes: Long) = dataStore.edit { it[Keys.storageLimit] = bytes }
    suspend fun setOfflineMode(enabled: Boolean) = dataStore.edit { it[Keys.offlineMode] = enabled }
    suspend fun setScrobble(enabled: Boolean) = dataStore.edit { it[Keys.scrobble] = enabled }
    suspend fun setShowOfflineBanner(enabled: Boolean) = dataStore.edit { it[Keys.showOfflineBanner] = enabled }
    suspend fun setAccentKey(key: String) = dataStore.edit { it[Keys.accentKey] = key }
}
