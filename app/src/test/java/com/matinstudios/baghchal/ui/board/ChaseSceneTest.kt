package com.matinstudios.baghchal.ui.board

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ChaseSceneTest {

    private val frames = (0 until ChaseScene.CYCLE_MILLIS step 10).map { ChaseScene.frameAt(it.toFloat()) }

    private fun tiger(frame: ChaseFrame) = frame.sprites.single { it.actor == Actor.TIGER }

    @Test
    fun `the loop starts and ends invisible`() {
        assertEquals(0f, ChaseScene.frameAt(0f).alpha)
        assertEquals(0f, ChaseScene.frameAt(ChaseScene.CYCLE_MILLIS - 1f).alpha)
    }

    @Test
    fun `the tiger leaps over a goat and that goat disappears`() {
        assertTrue(frames.any { tiger(it).lift > 2f })
        assertTrue(frames.any { it.sprites.count { s -> s.actor == Actor.GOAT } == 5 })
        assertEquals(4, frames.last().sprites.count { it.actor == Actor.GOAT })
        assertNotNull(frames.firstNotNullOfOrNull { it.burstX })
    }

    @Test
    fun `at the end the tiger is boxed in by goats on both sides and locked`() {
        val still = ChaseScene.STILL_FRAME
        val tigerX = tiger(still).x
        assertEquals(ChaseScene.TRAPPED_TIGER, tigerX)
        val goats = still.sprites.filter { it.actor == Actor.GOAT }.map { it.x }.sorted()
        assertEquals(ChaseScene.TRAPPED_GOATS.sorted(), goats)
        // Neighbours on both sides, and each jump is blocked by a goat or the end of the line.
        assertTrue(tigerX - 2 in goats && tigerX + 2 in goats)
        assertTrue(tigerX - 4 in goats && tigerX + 4 in goats)
        assertTrue(still.lockScale > 0f && still.alpha == 1f)
    }

    @Test
    fun `pieces on screen never stand on top of each other`() {
        for (frame in frames) {
            val resting = frame.sprites.filter { it.lift == 0f && it.alpha == 1f && it.x in 0f..ChaseScene.UNITS_WIDE }.map { it.x }
            for (i in resting.indices) for (j in i + 1 until resting.size) {
                assertTrue(abs(resting[i] - resting[j]) >= 1f, "pieces overlap: $resting")
            }
        }
    }
}
