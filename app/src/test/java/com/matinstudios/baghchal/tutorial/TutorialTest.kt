package com.matinstudios.baghchal.tutorial

import com.matinstudios.baghchal.engine.GameStatus
import com.matinstudios.baghchal.engine.Move
import com.matinstudios.baghchal.engine.Rules
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.engine.WinReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TutorialTest {

    /** Plays the script as a perfect student, step by step. */
    private fun walk(onStep: (TutorialState) -> Unit = {}): TutorialState {
        var state = Tutorial.start()
        while (true) {
            onStep(state)
            if (state.stage == Stage.INSTRUCTION) {
                state = solve(state)
                if (state.stage == Stage.TIGER_TURN) state = Tutorial.tigerReply(state)
            }
            assertEquals(Stage.DONE, state.stage, "step ${state.step.lesson}")
            state = Tutorial.next(state) ?: return state
        }
    }

    private fun solve(state: TutorialState): TutorialState = when (val goal = state.step.goal) {
        Goal.Next -> state
        Goal.SafePlacement -> Tutorial.tap(state, state.targets.min())
        is Goal.Play -> when (val move = goal.move) {
            is Move.Step -> Tutorial.tap(Tutorial.tap(state, move.from), move.to)
            else -> Tutorial.tap(state, move.to)
        }
    }

    @Test
    fun `every scripted move is legal where it is played`() {
        walk { state ->
            val step = state.step
            val goal = step.goal
            if (goal is Goal.Play) assertTrue(Rules.isLegal(state.game, goal.move), "${step.lesson}: ${goal.move}")
            val reply = step.reply ?: return@walk
            val afterPlayer = solve(state)
            assertEquals(Side.TIGER, afterPlayer.game.sideToMove)
            assertTrue(Rules.isLegal(afterPlayer.game, reply), "${step.lesson} reply: $reply")
        }
    }

    @Test
    fun `the lessons run in order and the last one ends with the goats winning`() {
        val lessons = mutableListOf<Lesson>()
        val end = walk { lessons += it.step.lesson }
        assertEquals(Lesson.entries.toList(), lessons.toList())
        assertTrue(end.isLastStep)
        assertEquals(GameStatus.Won(Side.GOAT, WinReason.TIGERS_TRAPPED), Rules.status(end.game))
        assertEquals(setOf(0, 4, 20, 24), end.trappedTigers)
        assertNull(Tutorial.next(end))
    }

    @Test
    fun `the meet step only needs Next`() {
        val start = Tutorial.start()
        assertEquals(Lesson.MEET, start.step.lesson)
        assertEquals(Stage.DONE, start.stage)
        assertEquals(start, Tutorial.tap(start, 12))
    }

    @Test
    fun `a tap off the glowing point is refused with a nudge`() {
        val firstGoat = Tutorial.next(Tutorial.start())!!
        assertEquals(setOf(12), firstGoat.targets)
        val refused = Tutorial.tap(firstGoat, 6)
        assertEquals(Nudge.TAP_GLOWING, refused.nudge)
        assertEquals(firstGoat.game, refused.game)

        val placed = Tutorial.tap(refused, 12)
        assertEquals(Stage.TIGER_TURN, placed.stage)
        assertNull(placed.nudge)
        assertEquals(Move.Place(12), placed.lastMove)
    }

    @Test
    fun `the block step shows the threatened jump and the careless step loses a goat`() {
        var state = Tutorial.next(Tutorial.start())!!
        state = Tutorial.tigerReply(Tutorial.tap(state, 12))
        state = Tutorial.next(state)!!
        assertEquals(Lesson.BLOCK, state.step.lesson)
        assertEquals(Move.Jump(8, 12, 16), state.arrow)
        assertEquals(Move.Jump(8, 12, 16), Rules.legalMoves(state.game.copy(sideToMove = Side.TIGER)).single { it is Move.Jump })

        state = Tutorial.next(Tutorial.tigerReply(Tutorial.tap(state, 16)))!!
        assertEquals(Lesson.CARELESS, state.step.lesson)
        state = Tutorial.tigerReply(Tutorial.tap(state, 7))
        assertEquals(1, state.game.goatsCaptured)
        assertEquals(Move.Jump(8, 7, 6), state.lastMove)
    }

    @Test
    fun `safe spot refuses a point a tiger could jump and shows the jump`() {
        var state = Tutorial.start()
        while (state.step.lesson != Lesson.SAFE_SPOT) {
            if (state.stage == Stage.INSTRUCTION) state = solve(state)
            if (state.stage == Stage.TIGER_TURN) state = Tutorial.tigerReply(state)
            state = Tutorial.next(state)!!
        }
        // The tiger on 0 jumps a goat on 6 into the empty centre; 1 is safe because 2 is filled.
        assertFalse(6 in state.targets)
        assertTrue(1 in state.targets)
        val refused = Tutorial.tap(state, 6)
        assertEquals(Nudge.TIGER_WOULD_EAT, refused.nudge)
        assertEquals(Move.Jump(0, 6, 12), refused.danger)
        assertEquals(Move.Jump(0, 6, 12), refused.arrow)

        val placed = Tutorial.tap(refused, 1)
        assertEquals(Stage.DONE, placed.stage)
        assertNull(placed.danger)
        assertEquals(Side.GOAT, placed.game.board[1])
    }

    @Test
    fun `trap step selects the goat before showing where it goes`() {
        var state = Tutorial.start()
        while (!state.isLastStep) {
            if (state.stage == Stage.INSTRUCTION) state = solve(state)
            if (state.stage == Stage.TIGER_TURN) state = Tutorial.tigerReply(state)
            state = Tutorial.next(state)!!
        }
        assertEquals(Move.Step(17, 18), state.arrow)
        assertEquals(emptySet<Int>(), state.targets)
        assertEquals(Nudge.TAP_GOAT, Tutorial.tap(state, 12).nudge)

        val selected = Tutorial.tap(state, 17)
        assertEquals(17, selected.selected)
        assertEquals(setOf(18), selected.targets)
        assertNull(selected.arrow)
        assertNotNull(Tutorial.tap(selected, 18).lastMove)
    }
}
