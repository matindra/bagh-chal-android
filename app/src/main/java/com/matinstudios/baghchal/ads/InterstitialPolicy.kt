package com.matinstudios.baghchal.ads

/**
 * Frequency cap for interstitials, which are only offered between games: at most once every
 * [minGamesBetween] finished games and at least [minIntervalMillis] apart. The interval also
 * counts from app start, so one never appears right after launch.
 */
class InterstitialPolicy(
    startedAtMillis: Long,
    private val minGamesBetween: Int = 2,
    private val minIntervalMillis: Long = 3 * 60_000L,
) {
    private var gamesSinceLast = 0
    private var lastShownAtMillis = startedAtMillis

    /** Call once per finished game; true if an interstitial should be shown now. */
    fun onGameFinished(nowMillis: Long): Boolean {
        gamesSinceLast++
        return gamesSinceLast >= minGamesBetween && nowMillis - lastShownAtMillis >= minIntervalMillis
    }

    /** Call when an interstitial was actually shown; a due ad that couldn't be shown stays due. */
    fun onShown(nowMillis: Long) {
        gamesSinceLast = 0
        lastShownAtMillis = nowMillis
    }
}
