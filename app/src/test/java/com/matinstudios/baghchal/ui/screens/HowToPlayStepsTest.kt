package com.matinstudios.baghchal.ui.screens

import com.matinstudios.baghchal.engine.Board
import com.matinstudios.baghchal.engine.GameState
import com.matinstudios.baghchal.engine.JumpLine
import com.matinstudios.baghchal.engine.Move
import com.matinstudios.baghchal.engine.Rules
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.engine.TOTAL_GOATS
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The rules pictures must never teach an illegal move. */
class HowToPlayStepsTest {

    private val steps = HOW_TO_PLAY_STEPS
    private fun illustration(index: Int) = steps[index].illustration

    @Test
    fun `five steps in the documented order`() {
        val moves = steps.map { it.illustration.move }
        assertEquals(5, moves.size)
        assertTrue(moves[0] is Move.Place, "placement")
        assertTrue(moves[1] is Move.Step && moves[1].let { (it as Move.Step).from } in illustration(1).goats, "goat move")
        assertTrue(moves[2] is Move.Step && moves[2].let { (it as Move.Step).from } in illustration(2).tigers, "tiger step")
        assertTrue(moves[3] is Move.Jump, "tiger capture")
        assertTrue(moves[4] is Move.Step && moves[4].let { (it as Move.Step).from } in illustration(4).goats, "trapping move by a goat")
    }

    @Test
    fun `positions are well formed`() {
        for (step in steps) {
            val ill = step.illustration
            assertEquals(4, ill.tigers.size, "four tigers")
            assertTrue(ill.tigers.intersect(ill.goats).isEmpty(), "one piece per point")
            assertTrue((ill.tigers + ill.goats).all { it in 0 until Board.POINTS })
        }
    }

    @Test
    fun `every move follows the rules`() {
        for (step in steps) {
            val ill = step.illustration
            val occupied = ill.tigers + ill.goats
            when (val move = ill.move) {
                is Move.Place -> assertTrue(move.to !in occupied, "place on an empty point")
                is Move.Step -> {
                    assertTrue(move.from in occupied, "a piece moves")
                    assertTrue(move.to !in occupied, "to an empty point")
                    assertTrue(Board.isAdjacent(move.from, move.to), "along a line, one step")
                }
                is Move.Jump -> {
                    assertTrue(move.from in ill.tigers, "only tigers capture")
                    assertTrue(move.over in ill.goats, "over a goat")
                    assertTrue(move.to !in occupied, "landing empty")
                    assertTrue(JumpLine(move.over, move.to) in Board.jumps[move.from], "straight line")
                }
            }
        }
    }

    @Test
    fun `the result of each move is shown`() {
        val place = illustration(0)
        assertTrue((place.move as Move.Place).to in place.goatsAfter)
        val capture = illustration(3)
        val jump = capture.move as Move.Jump
        assertTrue(jump.over !in capture.goatsAfter, "captured goat removed")
        assertTrue(jump.to in capture.tigersAfter && jump.from !in capture.tigersAfter, "tiger landed")
    }

    @Test
    fun `only the trapping picture shows a locked tiger, and its move is what traps it`() {
        val trap = illustration(4)
        assertEquals(setOf(0), trap.trappedAfter)
        val before = GameState.create(trap.tigers, trap.goats, Side.GOAT, goatsInHand = TOTAL_GOATS - trap.goats.size)
        assertEquals(emptySet(), Rules.trappedTigers(before), "before the move the tiger can still jump into 12")
        for (index in 0 until 4) assertEquals(emptySet(), illustration(index).trappedAfter, "step ${index + 1}")
    }
}
