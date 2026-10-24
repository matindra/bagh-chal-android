package com.matinstudios.baghchal.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EntitlementsTest {

    @Test
    fun `only wood is free`() {
        val free = Entitlements()
        assertTrue(free.isUnlocked(BoardTheme.WOOD))
        assertFalse(free.isUnlocked(BoardTheme.STONE))
        assertFalse(free.isUnlocked(BoardTheme.CLOTH))
    }

    @Test
    fun `a rewarded unlock opens just that theme`() {
        val stone = Entitlements(unlockedThemes = setOf(BoardTheme.STONE))
        assertTrue(stone.isUnlocked(BoardTheme.STONE))
        assertFalse(stone.isUnlocked(BoardTheme.CLOTH))
    }

    @Test
    fun `remove ads includes every theme`() {
        val owner = Entitlements(removeAds = true)
        assertTrue(BoardTheme.entries.all(owner::isUnlocked))
    }

    @Test
    fun `a selected theme that is not unlocked falls back to wood`() {
        // e.g. Remove Ads was refunded after Cloth was chosen.
        assertEquals(BoardTheme.WOOD, Entitlements().effectiveTheme(BoardTheme.CLOTH))
        assertEquals(BoardTheme.CLOTH, Entitlements(removeAds = true).effectiveTheme(BoardTheme.CLOTH))
        assertEquals(BoardTheme.STONE, Entitlements(unlockedThemes = setOf(BoardTheme.STONE)).effectiveTheme(BoardTheme.STONE))
    }
}
