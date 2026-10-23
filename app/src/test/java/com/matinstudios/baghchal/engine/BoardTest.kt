package com.matinstudios.baghchal.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BoardTest {

    /** Hand-written from the board diagram, independent of the generator. */
    private val expectedAdjacency = mapOf(
        0 to setOf(1, 5, 6),
        1 to setOf(0, 2, 6),
        2 to setOf(1, 3, 7, 6, 8),
        3 to setOf(2, 4, 8),
        4 to setOf(3, 9, 8),
        5 to setOf(0, 10, 6),
        6 to setOf(1, 5, 7, 11, 0, 2, 10, 12),
        7 to setOf(2, 6, 8, 12),
        8 to setOf(3, 7, 9, 13, 2, 4, 12, 14),
        9 to setOf(4, 8, 14),
        10 to setOf(5, 15, 11, 6, 16),
        11 to setOf(6, 10, 12, 16),
        12 to setOf(7, 11, 13, 17, 6, 8, 16, 18),
        13 to setOf(8, 12, 14, 18),
        14 to setOf(9, 13, 19, 8, 18),
        15 to setOf(10, 16, 20),
        16 to setOf(11, 15, 17, 21, 10, 12, 20, 22),
        17 to setOf(12, 16, 18, 22),
        18 to setOf(13, 17, 19, 23, 12, 14, 22, 24),
        19 to setOf(14, 18, 24),
        20 to setOf(15, 21, 16),
        21 to setOf(20, 22, 16),
        22 to setOf(21, 23, 17, 16, 18),
        23 to setOf(22, 24, 18),
        24 to setOf(23, 19, 18),
    )

    @Test
    fun `adjacency matches the board for all 25 points`() {
        for (point in 0 until Board.POINTS) {
            assertEquals(expectedAdjacency.getValue(point), Board.adjacency[point].toSet(), "point $point")
            assertEquals(Board.adjacency[point].size, Board.adjacency[point].toSet().size, "duplicates at $point")
        }
    }

    @Test
    fun `adjacency is symmetric`() {
        for (a in 0 until Board.POINTS) for (b in Board.adjacency[a]) {
            assertTrue(Board.isAdjacent(b, a), "$a-$b")
        }
    }

    @Test
    fun `board has 40 orthogonal and 16 diagonal lines`() {
        assertEquals(56 * 2, Board.adjacency.sumOf { it.size })
    }

    @Test
    fun `only even-parity points have diagonal neighbours`() {
        for (point in 0 until Board.POINTS) {
            val diagonal = Board.adjacency[point].filter {
                Board.row(it) != Board.row(point) && Board.col(it) != Board.col(point)
            }
            if (Board.hasDiagonals(point)) {
                assertTrue(diagonal.isNotEmpty(), "point $point should have diagonals")
            } else {
                assertTrue(diagonal.isEmpty(), "point $point should not have diagonals")
            }
        }
    }

    @Test
    fun `every jump is a straight line along existing connections`() {
        for (from in 0 until Board.POINTS) for ((over, landing) in Board.jumps[from]) {
            assertTrue(Board.isAdjacent(from, over), "$from over $over")
            assertTrue(Board.isAdjacent(over, landing), "$over to $landing")
            assertEquals(Board.row(over) - Board.row(from), Board.row(landing) - Board.row(over))
            assertEquals(Board.col(over) - Board.col(from), Board.col(landing) - Board.col(over))
        }
    }

    @Test
    fun `jump tables for sample points`() {
        assertEquals(
            setOf(JumpLine(1, 2), JumpLine(5, 10), JumpLine(6, 12)),
            Board.jumps[0].toSet(),
        )
        assertEquals(setOf(JumpLine(2, 3), JumpLine(6, 11)), Board.jumps[1].toSet())
        assertEquals(setOf(JumpLine(7, 8), JumpLine(11, 16), JumpLine(12, 18)), Board.jumps[6].toSet())
        assertEquals(
            setOf(
                JumpLine(7, 2), JumpLine(17, 22), JumpLine(11, 10), JumpLine(13, 14),
                JumpLine(6, 0), JumpLine(8, 4), JumpLine(16, 20), JumpLine(18, 24),
            ),
            Board.jumps[12].toSet(),
        )
        // Odd-parity point: no diagonal jumps, and no jump off the right edge.
        assertEquals(setOf(JumpLine(12, 11), JumpLine(8, 3), JumpLine(18, 23)), Board.jumps[13].toSet())
    }
}
