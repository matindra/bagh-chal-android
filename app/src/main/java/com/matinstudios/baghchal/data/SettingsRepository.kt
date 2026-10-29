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
enum class BoardTheme { WOOD, STONE, CLOTH, NIGHT, THANGKA, JUNGLE, MANDALA, DHAKA, BRASS, INK, SNOW }

/** Tiger and goat artwork, drawn everywhere a piece appears. Classic is free, the others are unlocked per [Entitlements]. */
enum class PieceSet { CLASSIC, TOKEN, GLYPH, FESTIVAL, SNOW, NIGHT_EYES, LINE_ART }

/** App colours: follow the phone's setting, or force light or dark. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    /** The game screens' mute button: silences music and sound effects during games only. */
    val gameMuted: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val boardTheme: BoardTheme = BoardTheme.WOOD,
    val pieceSet: PieceSet = PieceSet.CLASSIC,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<Settings> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            Settings(
                soundEnabled = prefs[SOUND] ?: true,
                musicEnabled = prefs[MUSIC] ?: true,
                gameMuted = prefs[GAME_MUTED] ?: false,
                hapticsEnabled = prefs[HAPTICS] ?: true,
                boardTheme = BoardTheme.entries.firstOrNull { it.name == prefs[BOARD_THEME] } ?: BoardTheme.WOOD,
                pieceSet = PieceSet.entries.firstOrNull { it.name == prefs[PIECE_SET] } ?: PieceSet.CLASSIC,
                themeMode = ThemeMode.entries.firstOrNull { it.name == prefs[THEME_MODE] } ?: ThemeMode.SYSTEM,
            )
        }

    /** Whether the first-launch welcome tutorial has been finished or skipped. */
    val tutorialSeen: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[TUTORIAL_SEEN] ?: false }

    suspend fun setTutorialSeen(seen: Boolean) {
        dataStore.edit { it[TUTORIAL_SEEN] = seen }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { it[SOUND] = enabled }
    }

    suspend fun setMusicEnabled(enabled: Boolean) {
        dataStore.edit { it[MUSIC] = enabled }
    }

    suspend fun setGameMuted(muted: Boolean) {
        dataStore.edit { it[GAME_MUTED] = muted }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStore.edit { it[HAPTICS] = enabled }
    }

    suspend fun setBoardTheme(theme: BoardTheme) {
        dataStore.edit { it[BOARD_THEME] = theme.name }
    }

    suspend fun setPieceSet(set: PieceSet) {
        dataStore.edit { it[PIECE_SET] = set.name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE] = mode.name }
    }

    private companion object {
        val SOUND = booleanPreferencesKey("settings_sound")
        val MUSIC = booleanPreferencesKey("settings_music")
        val HAPTICS = booleanPreferencesKey("settings_haptics")
        val GAME_MUTED = booleanPreferencesKey("settings_game_muted")
        val BOARD_THEME = stringPreferencesKey("settings_board_theme")
        val PIECE_SET = stringPreferencesKey("settings_piece_set")
        val THEME_MODE = stringPreferencesKey("settings_theme_mode")
        val TUTORIAL_SEEN = booleanPreferencesKey("tutorial_seen")
    }
}
