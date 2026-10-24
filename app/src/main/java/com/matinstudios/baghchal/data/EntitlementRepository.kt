package com.matinstudios.baghchal.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * What the player owns: the Remove Ads purchase (cached from Play Billing) and board themes
 * unlocked by watching a rewarded ad. Remove Ads includes every theme.
 */
data class Entitlements(
    val removeAds: Boolean = false,
    val unlockedThemes: Set<BoardTheme> = emptySet(),
) {
    fun isUnlocked(theme: BoardTheme): Boolean = theme == FREE_THEME || removeAds || theme in unlockedThemes

    /** The theme to draw: the player's choice, or the free one if that choice is (no longer) unlocked. */
    fun effectiveTheme(selected: BoardTheme): BoardTheme = if (isUnlocked(selected)) selected else FREE_THEME

    companion object {
        val FREE_THEME = BoardTheme.WOOD
    }
}

class EntitlementRepository(private val dataStore: DataStore<Preferences>) {

    val entitlements: Flow<Entitlements> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            Entitlements(
                removeAds = prefs[REMOVE_ADS] ?: false,
                unlockedThemes = prefs[UNLOCKED_THEMES].orEmpty()
                    .mapNotNull { name -> BoardTheme.entries.firstOrNull { it.name == name } }
                    .toSet(),
            )
        }

    suspend fun setRemoveAds(owned: Boolean) {
        dataStore.edit { it[REMOVE_ADS] = owned }
    }

    suspend fun unlockTheme(theme: BoardTheme) {
        dataStore.edit { it[UNLOCKED_THEMES] = it[UNLOCKED_THEMES].orEmpty() + theme.name }
    }

    /** For tests. */
    suspend fun reset() {
        dataStore.edit {
            it.remove(REMOVE_ADS)
            it.remove(UNLOCKED_THEMES)
        }
    }

    private companion object {
        val REMOVE_ADS = booleanPreferencesKey("entitlement_remove_ads")
        val UNLOCKED_THEMES = stringSetPreferencesKey("entitlement_unlocked_themes")
    }
}
