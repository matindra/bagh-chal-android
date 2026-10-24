package com.matinstudios.baghchal.ui.board

import androidx.compose.ui.geometry.Offset
import com.matinstudios.baghchal.engine.Move
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MoveArrowTest {

    private val size = 600f
    private fun center(point: Int) = BoardGeometry.pointCenter(point, size)
    private fun Offset.distanceTo(other: Offset) = (this - other).getDistance()
    private val pieceRadius = BoardGeometry.PIECE_SIZE / 2

    @Test
    fun `a step arrow runs straight from the origin to the destination`() {
        val arrow = MoveArrow.shape(Move.Step(7, 12), size)
        assertEquals(center(7).x, arrow.start.x, 0.01f)
        assertEquals(center(12).x, arrow.end.x, 0.01f)
        assertTrue(arrow.start.y > center(7).y && arrow.end.y < center(12).y, "starts and stops short of the pieces")
        assertTrue(arrow.end.distanceTo(center(12)) < pieceRadius, "head touches the arriving piece")
        assertEquals(center(7).x, arrow.control.x, 0.01f, "no curve")
    }

    @Test
    fun `a capture arrow arcs over the jumped goat`() {
        val arrow = MoveArrow.shape(Move.Jump(6, 7, 8), size) // left to right along row 2
        assertTrue(arrow.control.y < center(7).y - 50f, "arcs upward over the goat")
        assertEquals(center(7).x, arrow.control.x, 0.01f)
        assertTrue(arrow.end.distanceTo(center(8)) < pieceRadius)
    }

    @Test
    fun `a vertical capture arcs sideways`() {
        val arrow = MoveArrow.shape(Move.Jump(2, 7, 12), size)
        assertTrue(arrow.control.x < center(7).x - 50f)
    }

    @Test
    fun `a placement arrow comes in from the bottom edge`() {
        val arrow = MoveArrow.shape(Move.Place(12), size)
        assertEquals(center(12).x, arrow.start.x, 0.01f)
        assertTrue(arrow.start.y > center(22).y, "starts below the last row")
        assertTrue(arrow.end.y > center(12).y, "points up into the target")
    }

    @Test
    fun `the head is a symmetric triangle behind the tip`() {
        for (move in listOf(Move.Step(0, 6), Move.Jump(6, 7, 8), Move.Place(3))) {
            val arrow = MoveArrow.shape(move, size)
            assertEquals(arrow.end.distanceTo(arrow.headLeft), arrow.end.distanceTo(arrow.headRight), 0.01f, "$move")
            val tipToControl = arrow.control - arrow.end
            val baseMid = (arrow.headLeft + arrow.headRight) / 2f
            val tipToBase = baseMid - arrow.end
            assertTrue(tipToBase.x * tipToControl.x + tipToBase.y * tipToControl.y > 0f, "head points along the arrow for $move")
        }
    }
}
