package com.matinstudios.baghchal.online

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RoomCleanupTest {

    @Test
    fun `first sweep is due, then only after the interval`() {
        assertTrue(RoomCleanup.isDue(lastSweepAt = null, now = 1_000L))
        assertFalse(RoomCleanup.isDue(lastSweepAt = 1_000L, now = 1_000L + RoomCleanup.SWEEP_INTERVAL_MILLIS - 1))
        assertTrue(RoomCleanup.isDue(lastSweepAt = 1_000L, now = 1_000L + RoomCleanup.SWEEP_INTERVAL_MILLIS))
    }

    @Test
    fun `cutoffs leave a clock margin past the max age`() {
        val now = 10_000_000_000L
        assertEquals(now - RoomCleanup.ROOM_MAX_AGE_MILLIS - RoomCleanup.CLOCK_MARGIN_MILLIS, RoomCleanup.roomCutoff(now))
        assertEquals(now - RoomCleanup.CODE_MAX_AGE_MILLIS - RoomCleanup.CLOCK_MARGIN_MILLIS, RoomCleanup.codeCutoff(now))
    }

    @Test
    fun `security rules use the same ages and batch size`() {
        val rules = listOf(File("../firebase/database.rules.json"), File("firebase/database.rules.json"))
            .first { it.exists() }
            .readText()
        val rooms = "query.orderByChild === 'createdAt' && query.endAt <= now - ${RoomCleanup.ROOM_MAX_AGE_MILLIS} " +
            "&& query.limitToFirst <= ${RoomCleanup.BATCH}"
        val codes = "query.orderByChild === 'at' && query.endAt <= now - ${RoomCleanup.CODE_MAX_AGE_MILLIS} " +
            "&& query.limitToFirst <= ${RoomCleanup.BATCH}"
        assertTrue(rooms in rules, "rooms listing rule")
        assertTrue(codes in rules, "codes listing rule")
        assertTrue("data.child('createdAt').val() <= now - ${RoomCleanup.ROOM_MAX_AGE_MILLIS}" in rules, "room delete rule")
    }
}
