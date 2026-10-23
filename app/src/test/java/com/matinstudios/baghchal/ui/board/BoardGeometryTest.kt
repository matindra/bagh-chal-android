package com.matinstudios.baghchal.ui.board

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BoardGeometryTest {

    @Test
    fun `point centers use the artwork coordinates`() {
        val coords = listOf(75f, 187.5f, 300f, 412.5f, 525f)
        for (point in 0 until 25) {
            assertEquals(Offset(coords[point % 5], coords[point / 5]), BoardGeometry.pointCenter(point, 600f))
        }
    }

    @Test
    fun `point centers scale with the board size`() {
        assertEquals(Offset(37.5f, 37.5f), BoardGeometry.pointCenter(0, 300f))
        assertEquals(Offset(262.5f, 262.5f), BoardGeometry.pointCenter(24, 300f))
    }

    @Test
    fun `every point center maps back to its point at any size`() {
        for (size in listOf(300f, 600f, 1080f)) for (point in 0 until 25) {
            assertEquals(point, BoardGeometry.pointAt(BoardGeometry.pointCenter(point, size), size))
        }
    }

    @Test
    fun `taps near a point select it, taps between points or outside do not`() {
        assertEquals(12, BoardGeometry.pointAt(Offset(300f + 40f, 300f - 30f), 600f))
        assertNull(BoardGeometry.pointAt(Offset(243.75f, 243.75f), 600f), "between points")
        assertNull(BoardGeometry.pointAt(Offset(5f, 5f), 600f), "board border")
        assertNull(BoardGeometry.pointAt(Offset(-50f, 300f), 600f), "outside")
        assertNull(BoardGeometry.pointAt(Offset(0f, 0f), 0f), "empty board")
    }
}
