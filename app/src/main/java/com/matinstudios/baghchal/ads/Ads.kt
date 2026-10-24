package com.matinstudios.baghchal.ads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Each rewarded feature has its own ad unit, so AdMob reports them separately. */
enum class RewardedPlacement { UNDO, HINT, THEME }

/** How a rewarded-ad request ended. */
enum class RewardOutcome {
    /** The player watched the ad to the end. */
    EARNED,

    /** The player closed the ad early. */
    DISMISSED,

    /** No ad could be shown (offline, no fill, no consent, ads disabled). */
    UNAVAILABLE,
}

/**
 * Undo and Hint must work offline, so they are granted whenever no ad could be shown; only
 * closing the ad early withholds them.
 */
fun RewardOutcome.grantsAssist(): Boolean = this != RewardOutcome.DISMISSED

/** A theme unlock is permanent, so it needs a fully watched ad. */
fun RewardOutcome.grantsThemeUnlock(): Boolean = this == RewardOutcome.EARNED

/** The app's ads, as the screens see them. Every call fails silently: the game works with no ads. */
interface Ads {
    /** Banners may be shown: the SDK is initialised with consent, and Remove Ads isn't owned. */
    val bannersEnabled: StateFlow<Boolean>

    /** A rewarded ad for [placement] is loaded and would be shown; never true with Remove Ads. */
    fun rewardedReady(placement: RewardedPlacement): StateFlow<Boolean>

    /** Shows the rewarded ad for [placement] over the current activity and waits for it to close. */
    suspend fun showRewarded(placement: RewardedPlacement): RewardOutcome

    /** The player has finished a game and is leaving its result: shows an interstitial if one is due. */
    fun onGameFinished()
}

/** No ads at all: used by instrumented tests. */
object NoAds : Ads {
    override val bannersEnabled: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
    private val never: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
    override fun rewardedReady(placement: RewardedPlacement) = never
    override suspend fun showRewarded(placement: RewardedPlacement) = RewardOutcome.UNAVAILABLE
    override fun onGameFinished() = Unit
}
