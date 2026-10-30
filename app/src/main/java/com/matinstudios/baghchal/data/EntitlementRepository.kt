package com.matinstudios.baghchal.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** How the player is ad-free right now, strongest first in [Entitlements.adFree]. */
enum class AdFree { NONE, DAY_PASS, YEARLY, LIFETIME }

/** A day pass lasts this long from the rewarded ad that earned it. */
const val DAY_PASS_MILLIS = 24 * 60 * 60 * 1000L

/**
 * What the player owns: the lifetime Remove Ads purchase and the yearly ad-free subscription
 * (both cached from Play Billing), the 24-hour pass earned with a rewarded ad, and the board themes
 * and piece sets unlocked by rewarded ads. Only lifetime Remove Ads includes every theme and piece set.
 *
 * @property dayPassUntilMillis wall-clock end of the day pass (0 = never earned).
 */
data class Entitlements(
    val removeAds: Boolean = false,
    val yearlyPass: Boolean = false,
    val dayPassUntilMillis: Long = 0L,
    val unlockedThemes: Set<BoardTheme> = emptySet(),
    val unlockedPieceSets: Set<PieceSet> = emptySet(),
) {
    fun adFree(nowMillis: Long): AdFree = when {
        removeAds -> AdFree.LIFETIME
        yearlyPass -> AdFree.YEARLY
        dayPassLeftMillis(nowMillis) > 0 -> AdFree.DAY_PASS
        else -> AdFree.NONE
    }

    /**
     * Time left on the day pass, or 0. An end more than a day away means the clock was turned
     * back, so the pass doesn't count.
     */
    fun dayPassLeftMillis(nowMillis: Long): Long {
        val left = dayPassUntilMillis - nowMillis
        return if (left in 1..DAY_PASS_MILLIS) left else 0L
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
                dayPassUntilMillis = prefs[DAY_PASS_UNTIL] ?: 0L,
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

    /** Starts (or restarts) the 24-hour pass at [nowMillis]. */
    suspend fun startDayPass(nowMillis: Long) {
        dataStore.edit { it[DAY_PASS_UNTIL] = nowMillis + DAY_PASS_MILLIS }
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
            it.remove(DAY_PASS_UNTIL)
            it.remove(UNLOCKED_THEMES)
            it.remove(UNLOCKED_PIECE_SETS)
        }
    }

    private companion object {
        val REMOVE_ADS = booleanPreferencesKey("entitlement_remove_ads")
        val YEARLY_PASS = booleanPreferencesKey("entitlement_yearly_pass")
        val DAY_PASS_UNTIL = longPreferencesKey("entitlement_day_pass_until")
        val UNLOCKED_THEMES = stringSetPreferencesKey("entitlement_unlocked_themes")
        val UNLOCKED_PIECE_SETS = stringSetPreferencesKey("entitlement_unlocked_piece_sets")
    }
}
