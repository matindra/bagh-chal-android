package com.matinstudios.baghchal.ads

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InterstitialPolicyTest {

    private val minute = 60_000L

    @Test
    fun `due after every second game once three minutes have passed`() {
        val policy = InterstitialPolicy(startedAtMillis = 0)
        assertFalse(policy.onGameFinished(10 * minute), "first game")
        assertTrue(policy.onGameFinished(11 * minute), "second game")
        policy.onShown(11 * minute)
        assertFalse(policy.onGameFinished(20 * minute))
        assertTrue(policy.onGameFinished(21 * minute))
    }

    @Test
    fun `quick games wait for the three minute gap`() {
        val policy = InterstitialPolicy(startedAtMillis = 0)
        policy.onGameFinished(minute)
        assertFalse(policy.onGameFinished(2 * minute), "two games, but only 2 minutes since launch")
        assertTrue(policy.onGameFinished(3 * minute))
        policy.onShown(3 * minute)
        policy.onGameFinished(4 * minute)
        assertFalse(policy.onGameFinished(5 * minute), "2 minutes since the last ad")
        assertTrue(policy.onGameFinished(6 * minute))
    }

    @Test
    fun `an ad that could not be shown stays due`() {
        val policy = InterstitialPolicy(startedAtMillis = 0)
        policy.onGameFinished(10 * minute)
        assertTrue(policy.onGameFinished(11 * minute))
        // Nothing was loaded, so onShown was never called.
        assertTrue(policy.onGameFinished(12 * minute))
    }
}
