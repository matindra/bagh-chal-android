package com.matinstudios.baghchal.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Board artwork; all share the same geometry. Wood is free, the others are unlocked per [Entitlements]. */
enum class BoardTheme { WOOD, STONE, CLOTH }

data class Settings(
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val boardTheme: BoardTheme = BoardTheme.WOOD,
)

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<Settings> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            Settings(
                soundEnabled = prefs[SOUND] ?: true,
                hapticsEnabled = prefs[HAPTICS] ?: true,
                boardTheme = BoardTheme.entries.firstOrNull { it.name == prefs[BOARD_THEME] } ?: BoardTheme.WOOD,
            )
        }

    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { it[SOUND] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStore.edit { it[HAPTICS] = enabled }
    }

    suspend fun setBoardTheme(theme: BoardTheme) {
        dataStore.edit { it[BOARD_THEME] = theme.name }
    }

    private companion object {
        val SOUND = booleanPreferencesKey("settings_sound")
        val HAPTICS = booleanPreferencesKey("settings_haptics")
        val BOARD_THEME = stringPreferencesKey("settings_board_theme")
    }
}
