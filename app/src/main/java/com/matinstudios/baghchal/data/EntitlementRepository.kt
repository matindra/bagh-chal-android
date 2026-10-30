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

/** How the player is ad-free right now, strongest first in [Entitlements.adFree]. */
enum class AdFree { NONE, YEARLY, LIFETIME }

/**
 * What the player owns: the lifetime Remove Ads purchase and the yearly ad-free subscription
 * (both cached from Play Billing), and the board themes and piece sets unlocked by rewarded ads.
 * Only lifetime Remove Ads includes every theme and piece set.
 */
data class Entitlements(
    val removeAds: Boolean = false,
    val yearlyPass: Boolean = false,
    val unlockedThemes: Set<BoardTheme> = emptySet(),
    val unlockedPieceSets: Set<PieceSet> = emptySet(),
) {
    fun adFree(): AdFree = when {
        removeAds -> AdFree.LIFETIME
        yearlyPass -> AdFree.YEARLY
        else -> AdFree.NONE
    }

    fun isUnlocked(theme: BoardTheme): Boolean = theme == FREE_THEME || removeAds || theme in unlockedThemes

    fun isUnlocked(set: PieceSet): Boolean = set == FREE_PIECE_SET || removeAds || set in unlockedPieceSets

    /** The theme to draw: the player's choice, or the free one if that choice is (no longer) unlocked. */
    fun effectiveTheme(selected: BoardTheme): BoardTheme = if (isUnlocked(selected)) selected else FREE_THEME

    /** The pieces to draw: the player's choice, or the free set if that choice is (no longer) unlocked. */
    fun effectivePieceSet(selected: PieceSet): PieceSet = if (isUnlocked(selected)) selected else FREE_PIECE_SET

    companion object {
        val FREE_THEME = BoardTheme.WOOD
        val FREE_PIECE_SET = PieceSet.CLASSIC
    }
}

class EntitlementRepository(private val dataStore: DataStore<Preferences>) {

    val entitlements: Flow<Entitlements> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            Entitlements(
                removeAds = prefs[REMOVE_ADS] ?: false,
                yearlyPass = prefs[YEARLY_PASS] ?: false,
                unlockedThemes = prefs[UNLOCKED_THEMES].orEmpty()
                    .mapNotNull { name -> BoardTheme.entries.firstOrNull { it.name == name } }
                    .toSet(),
                unlockedPieceSets = prefs[UNLOCKED_PIECE_SETS].orEmpty()
                    .mapNotNull { name -> PieceSet.entries.firstOrNull { it.name == name } }
                    .toSet(),
            )
        }

    suspend fun setRemoveAds(owned: Boolean) {
        dataStore.edit { it[REMOVE_ADS] = owned }
    }

    suspend fun setYearlyPass(active: Boolean) {
        dataStore.edit { it[YEARLY_PASS] = active }
    }

    suspend fun unlockTheme(theme: BoardTheme) {
        dataStore.edit { it[UNLOCKED_THEMES] = it[UNLOCKED_THEMES].orEmpty() + theme.name }
    }

    suspend fun unlockPieceSet(set: PieceSet) {
        dataStore.edit { it[UNLOCKED_PIECE_SETS] = it[UNLOCKED_PIECE_SETS].orEmpty() + set.name }
    }

    /** For tests. */
    suspend fun reset() {
        dataStore.edit {
            it.remove(REMOVE_ADS)
            it.remove(YEARLY_PASS)
            it.remove(UNLOCKED_THEMES)
            it.remove(UNLOCKED_PIECE_SETS)
        }
    }

    private companion object {
        val REMOVE_ADS = booleanPreferencesKey("entitlement_remove_ads")
        val YEARLY_PASS = booleanPreferencesKey("entitlement_yearly_pass")
        val UNLOCKED_THEMES = stringSetPreferencesKey("entitlement_unlocked_themes")
        val UNLOCKED_PIECE_SETS = stringSetPreferencesKey("entitlement_unlocked_piece_sets")
    }
}
