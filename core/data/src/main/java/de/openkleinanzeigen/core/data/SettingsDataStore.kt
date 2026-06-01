package de.openkleinanzeigen.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.openkleinanzeigen.core.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

class SettingsDataStore(private val context: Context) {
    private object Keys {
        val pollInterval = longPreferencesKey("poll_interval_seconds")
        val updateInterval = longPreferencesKey("update_interval_seconds")
        val autoUpdate = booleanPreferencesKey("auto_update")
        val debugLogging = booleanPreferencesKey("debug_logging")
        val notifyListings = booleanPreferencesKey("notify_listings")
        val notifyMessages = booleanPreferencesKey("notify_messages")
        val notifyAgents = booleanPreferencesKey("notify_agents")
        val notifyBackend = booleanPreferencesKey("notify_backend")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            pollIntervalSeconds = prefs[Keys.pollInterval] ?: 300L,
            updateCheckIntervalSeconds = prefs[Keys.updateInterval] ?: 5L,
            autoUpdateEnabled = prefs[Keys.autoUpdate] ?: false,
            debugLoggingEnabled = prefs[Keys.debugLogging] ?: false,
            notifyNewListings = prefs[Keys.notifyListings] ?: true,
            notifyMessages = prefs[Keys.notifyMessages] ?: true,
            notifyAgentMatches = prefs[Keys.notifyAgents] ?: true,
            notifyBackendAgents = prefs[Keys.notifyBackend] ?: true,
        )
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val current = AppSettings(
                pollIntervalSeconds = prefs[Keys.pollInterval] ?: 300L,
                updateCheckIntervalSeconds = prefs[Keys.updateInterval] ?: 5L,
                autoUpdateEnabled = prefs[Keys.autoUpdate] ?: false,
                debugLoggingEnabled = prefs[Keys.debugLogging] ?: false,
                notifyNewListings = prefs[Keys.notifyListings] ?: true,
                notifyMessages = prefs[Keys.notifyMessages] ?: true,
                notifyAgentMatches = prefs[Keys.notifyAgents] ?: true,
                notifyBackendAgents = prefs[Keys.notifyBackend] ?: true,
            )
            val updated = transform(current)
            prefs[Keys.pollInterval] = updated.pollIntervalSeconds
            prefs[Keys.updateInterval] = updated.updateCheckIntervalSeconds
            prefs[Keys.autoUpdate] = updated.autoUpdateEnabled
            prefs[Keys.debugLogging] = updated.debugLoggingEnabled
            prefs[Keys.notifyListings] = updated.notifyNewListings
            prefs[Keys.notifyMessages] = updated.notifyMessages
            prefs[Keys.notifyAgents] = updated.notifyAgentMatches
            prefs[Keys.notifyBackend] = updated.notifyBackendAgents
        }
    }
}
