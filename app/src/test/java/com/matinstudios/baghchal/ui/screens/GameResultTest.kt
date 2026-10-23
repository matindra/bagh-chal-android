package com.matinstudios.baghchal.ui.screens

import com.matinstudios.baghchal.engine.GameState
import com.matinstudios.baghchal.engine.Move
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.engine.ai.Difficulty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GameResultTest {

    private val setup = GameSetup.vsComputer(Side.GOAT, Difficulty.HARD)

    /** Goats to move; goat 7 -> 2 traps every tiger. */
    private fun almostTrapped() = GameInteraction.fromGame(
        GameState.create(
            tigers = listOf(0, 4, 20, 24),
            goats = listOf(1, 3, 5, 6, 7, 8, 9, 10, 12, 14, 15, 16, 18, 19, 21, 22, 23),
            sideToMove = Side.GOAT,
            goatsInHand = 0,
            goatsCaptured = 3,
            moveCount = 40,
        ),
        aiSide = Side.TIGER,
    )

    @Test
    fun `the winning move produces a result with the winner and move count`() {
        val before = almostTrapped()
        val after = GameInteraction.applyMove(before, Move.Step(7, 2))
        assertEquals(GameResult(Side.GOAT, 41, setup), resultFor(before, after, setup))
    }

    @Test
    fun `tigers winning by capture`() {
        val goats = listOf(13, 1, 2, 3, 5, 6, 8, 9, 10, 15, 16, 17, 19, 21, 22, 23)
        val before = GameInteraction.fromGame(
            GameState.create(listOf(12, 4, 20, 24), goats, Side.TIGER, goatsInHand = 0, goatsCaptured = 4, moveCount = 7),
            aiSide = null,
        )
        val after = GameInteraction.applyMove(before, Move.Jump(12, 13, 14))
        assertEquals(GameResult(Side.TIGER, 8, GameSetup.TWO_PLAYER), resultFor(before, after, GameSetup.TWO_PLAYER))
    }

    @Test
    fun `no result for ordinary moves, repeats or draws`() {
        val start = GameInteraction.newGame()
        val placed = GameInteraction.applyMove(start, Move.Place(12))
        assertNull(resultFor(start, placed, setup))

        val won = GameInteraction.applyMove(almostTrapped(), Move.Step(7, 2))
        assertNull(resultFor(won, won.copy(isAiThinking = false), setup), "only once")
        assertNull(resultFor(null, won, setup), "not when opening an already finished game")

        val drawn = GameInteraction.fromGame(
            GameState.create(listOf(0, 4, 20, 24), listOf(12), Side.GOAT, goatsInHand = 19, pliesSinceCapture = 100),
            aiSide = null,
        )
        assertNull(resultFor(start, drawn, setup), "draws stay on the game screen")
    }

    @Test
    fun `route round-trips through navigation arguments`() {
        for (result in listOf(
            GameResult(Side.TIGER, 1, GameSetup.TWO_PLAYER),
            GameResult(Side.GOAT, 123, GameSetup.vsComputer(Side.TIGER, Difficulty.EASY)),
        )) {
            val args = result.route.substringAfter('?').split('&').associate { it.substringBefore('=') to it.substringAfter('=') }
            assertEquals(
                result,
                GameResult.fromArgs(
                    args[GameResult.ARG_WINNER],
                    args.getValue(GameResult.ARG_MOVES).toInt(),
                    args[GameSetup.ARG_AI],
                    args[GameSetup.ARG_DIFFICULTY],
                ),
            )
        }
    }

    @Test
    fun `screen state lists trapped tigers`() {
        val before = almostTrapped()
        assertEquals(setOf(20, 24), before.trappedTigers)
        val after = GameInteraction.applyMove(before, Move.Step(7, 2))
        assertEquals(setOf(0, 4, 20, 24), after.trappedTigers)
        assertEquals(setOf(0, 4, 20, 24), GameInteraction.onPointTapped(after, 3).trappedTigers, "kept on taps")
    }
}
