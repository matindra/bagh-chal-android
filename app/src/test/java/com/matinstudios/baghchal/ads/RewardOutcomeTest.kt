package com.matinstudios.baghchal.ads

import kotlin.test.Test
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
}
