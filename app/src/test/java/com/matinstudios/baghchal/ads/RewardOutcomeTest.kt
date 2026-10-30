package com.matinstudios.baghchal.ads

import com.matinstudios.baghchal.data.AdFree
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RewardOutcomeTest {

    @Test
    fun `undo and hint work offline but not when the ad is skipped`() {
        assertTrue(RewardOutcome.EARNED.grantsAssist())
        assertTrue(RewardOutcome.UNAVAILABLE.grantsAssist())
        assertFalse(RewardOutcome.DISMISSED.grantsAssist())
    }

    @Test
    fun `a permanent theme unlock needs a fully watched ad`() {
        assertTrue(RewardOutcome.EARNED.grantsThemeUnlock())
        assertFalse(RewardOutcome.UNAVAILABLE.grantsThemeUnlock())
        assertFalse(RewardOutcome.DISMISSED.grantsThemeUnlock())
    }

    @Test
    fun `the ad-free day needs a fully watched ad`() {
        assertTrue(RewardOutcome.EARNED.grantsDayPass())
        assertFalse(RewardOutcome.UNAVAILABLE.grantsDayPass())
        assertFalse(RewardOutcome.DISMISSED.grantsDayPass())
    }

    @Test
    fun `every rewarded ad is offered to players who are not ad-free`() {
        assertTrue(RewardedPlacement.entries.all { it.offeredWhen(AdFree.NONE) })
    }

    @Test
    fun `on a pass only board and piece unlocks offer an ad`() {
        for (pass in listOf(AdFree.DAY_PASS, AdFree.YEARLY)) {
            assertEquals(listOf(RewardedPlacement.THEME), RewardedPlacement.entries.filter { it.offeredWhen(pass) }, pass.name)
        }
    }

    @Test
    fun `lifetime remove ads offers no rewarded ads`() {
        assertTrue(RewardedPlacement.entries.none { it.offeredWhen(AdFree.LIFETIME) })
    }
}
