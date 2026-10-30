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
        assertEquals(AdFree.NONE, Entitlements().adFree())
    }

    @Test
    fun `the strongest ad-free kind wins`() {
        assertEquals(AdFree.YEARLY, Entitlements(yearlyPass = true).adFree())
        assertEquals(AdFree.LIFETIME, Entitlements(yearlyPass = true, removeAds = true).adFree())
    }

    @Test
    fun `the yearly pass does not unlock boards or pieces`() {
        val pass = Entitlements(yearlyPass = true)
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
