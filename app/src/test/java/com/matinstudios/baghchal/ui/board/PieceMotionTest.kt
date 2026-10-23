package com.matinstudios.baghchal.ui.board

import com.matinstudios.baghchal.engine.Move
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PieceMotionTest {

    private val size = 600f
    private fun center(point: Int) = BoardGeometry.pointCenter(point, size)

    @Test
    fun `a step slides in a straight line from start to end`() {
        val step = Move.Step(6, 12)
        assertEquals(center(6), PieceMotion.center(step, 0f, size))
        assertEquals(center(12), PieceMotion.center(step, 1f, size))
        val midway = PieceMotion.center(step, 0.5f, size)
        assertEquals((center(6).x + center(12).x) / 2, midway.x, 0.01f)
        assertEquals((center(6).y + center(12).y) / 2, midway.y, 0.01f)
        assertEquals(1f, PieceMotion.scale(step, 0.5f))
    }

    @Test
    fun `a jump starts and lands on its points and peaks above the captured goat`() {
        val jump = Move.Jump(10, 11, 12) // horizontal, so the arc is purely vertical
        assertEquals(center(10).x, PieceMotion.center(jump, 0f, size).x, 0.01f)
        assertEquals(center(10).y, PieceMotion.center(jump, 0f, size).y, 0.01f)
        assertEquals(center(12).x, PieceMotion.center(jump, 1f, size).x, 0.01f)
        assertEquals(center(12).y, PieceMotion.center(jump, 1f, size).y, 0.01f)

        val peak = PieceMotion.center(jump, 0.5f, size)
        assertEquals(center(11).x, peak.x, 0.01f)
        assertEquals(center(11).y - PieceMotion.JUMP_ARC_HEIGHT, peak.y, 0.01f)
        assertTrue(PieceMotion.scale(jump, 0.5f) > PieceMotion.scale(jump, 0f))
        assertEquals(1f, PieceMotion.scale(jump, 1f), 0.001f)
    }

    @Test
    fun `a jump along the top row stays on the board`() {
        for (p in listOf(0.25f, 0.5f, 0.75f)) {
            val jump = Move.Jump(0, 1, 2)
            val top = PieceMotion.center(jump, p, size).y - BoardGeometry.PIECE_SIZE / 2 * PieceMotion.scale(jump, p)
            assertTrue(top >= -0.01f, "piece top at $top for progress $p")
            assertTrue(PieceMotion.center(jump, p, size).y < center(1).y, "still lifts a little")
        }
    }

    @Test
    fun `the arc scales with the board`() {
        val jump = Move.Jump(10, 11, 12)
        val small = PieceMotion.center(jump, 0.5f, 300f)
        assertEquals(BoardGeometry.pointCenter(11, 300f).y - PieceMotion.JUMP_ARC_HEIGHT / 2, small.y, 0.01f)
    }

    @Test
    fun `captured goat fades out and a placed goat fades and grows in`() {
        assertEquals(1f, PieceMotion.capturedAlpha(0f))
        assertEquals(0f, PieceMotion.capturedAlpha(1f))
        val place = Move.Place(7)
        assertEquals(center(7), PieceMotion.center(place, 0.3f, size))
        assertEquals(0f, PieceMotion.alpha(place, 0f))
        assertEquals(1f, PieceMotion.alpha(place, 1f))
        assertTrue(PieceMotion.scale(place, 0f) < 1f)
        assertEquals(1f, PieceMotion.scale(place, 1f))
    }

    @Test
    fun `captures take longest, placements are quickest`() {
        assertTrue(PieceMotion.durationMillis(Move.Jump(0, 1, 2)) > PieceMotion.durationMillis(Move.Step(0, 1)))
        assertTrue(PieceMotion.durationMillis(Move.Step(0, 1)) > PieceMotion.durationMillis(Move.Place(1)))
    }
}
