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
        assertEquals(listOf(BoardTheme.WOOD), BoardTheme.entries.filter(free::isUnlocked))
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

    @Test
    fun `nothing owned means ads`() {
        assertEquals(AdFree.NONE, Entitlements().adFree(nowMillis = 1_000L))
    }

    @Test
    fun `the strongest ad-free kind wins`() {
        val now = 1_000_000L
        val day = Entitlements(dayPassUntilMillis = now + 60_000L)
        assertEquals(AdFree.DAY_PASS, day.adFree(now))
        assertEquals(AdFree.YEARLY, day.copy(yearlyPass = true).adFree(now))
        assertEquals(AdFree.LIFETIME, day.copy(yearlyPass = true, removeAds = true).adFree(now))
    }

    @Test
    fun `a day pass lasts 24 hours`() {
        val start = 5_000_000L
        val pass = Entitlements(dayPassUntilMillis = start + DAY_PASS_MILLIS)
        assertEquals(DAY_PASS_MILLIS, pass.dayPassLeftMillis(start))
        assertEquals(AdFree.DAY_PASS, pass.adFree(start + DAY_PASS_MILLIS - 1))
        assertEquals(AdFree.NONE, pass.adFree(start + DAY_PASS_MILLIS))
    }

    @Test
    fun `turning the clock back does not stretch a day pass`() {
        val start = 5_000_000L
        val pass = Entitlements(dayPassUntilMillis = start + DAY_PASS_MILLIS)
        assertEquals(AdFree.NONE, pass.adFree(start - 1))
        assertEquals(0L, pass.dayPassLeftMillis(start - 1))
    }

    @Test
    fun `passes do not unlock boards or pieces`() {
        val pass = Entitlements(yearlyPass = true, dayPassUntilMillis = Long.MAX_VALUE)
        assertEquals(listOf(BoardTheme.WOOD), BoardTheme.entries.filter(pass::isUnlocked))
        assertEquals(listOf(PieceSet.CLASSIC), PieceSet.entries.filter(pass::isUnlocked))
    }

    @Test
    fun `only classic pieces are free`() {
        assertEquals(listOf(PieceSet.CLASSIC), PieceSet.entries.filter(Entitlements()::isUnlocked))
    }

    @Test
    fun `a rewarded unlock opens just that piece set, and remove ads opens them all`() {
        val glyph = Entitlements(unlockedPieceSets = setOf(PieceSet.GLYPH))
        assertTrue(glyph.isUnlocked(PieceSet.GLYPH))
        assertFalse(glyph.isUnlocked(PieceSet.TOKEN))
        assertTrue(PieceSet.entries.all(Entitlements(removeAds = true)::isUnlocked))
    }

    @Test
    fun `a selected piece set that is not unlocked falls back to classic`() {
        assertEquals(PieceSet.CLASSIC, Entitlements().effectivePieceSet(PieceSet.FESTIVAL))
        assertEquals(PieceSet.FESTIVAL, Entitlements(removeAds = true).effectivePieceSet(PieceSet.FESTIVAL))
        assertEquals(PieceSet.SNOW, Entitlements(unlockedPieceSets = setOf(PieceSet.SNOW)).effectivePieceSet(PieceSet.SNOW))
    }
}
