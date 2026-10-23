package com.matinstudios.baghchal.ui.screens

import com.matinstudios.baghchal.engine.GameState
import com.matinstudios.baghchal.engine.GameStatus
import com.matinstudios.baghchal.engine.Move
import com.matinstudios.baghchal.engine.Rules
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.engine.WinReason
import com.matinstudios.baghchal.engine.ai.Difficulty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class GameInteractionTest {

    private fun GameUiState.tap(vararg points: Int) =
        points.fold(this) { ui, point -> GameInteraction.onPointTapped(ui, point) }

    @Test
    fun `new game highlights every empty point for goat placement`() {
        val ui = GameInteraction.newGame()
        assertNull(ui.selected)
        assertEquals((0 until 25).toSet() - setOf(0, 4, 20, 24), ui.targets)
    }

    @Test
    fun `tapping an empty point places a goat and passes the turn`() {
        val ui = GameInteraction.newGame().tap(12)
        assertEquals(Side.GOAT, ui.game.board[12])
        assertEquals(Side.TIGER, ui.game.sideToMove)
        assertEquals(emptySet(), ui.targets, "nothing to highlight until a tiger is selected")
    }

    @Test
    fun `tapping a tiger selects it and highlights its moves`() {
        val ui = GameInteraction.newGame().tap(1, 0)
        assertEquals(0, ui.selected)
        assertEquals(setOf(5, 6, 2), ui.targets, "steps to 5 and 6, capture over 1 to 2")
    }

    @Test
    fun `tapping a highlighted target moves the selected piece`() {
        val ui = GameInteraction.newGame().tap(1, 0, 2)
        assertEquals(Side.TIGER, ui.game.board[2])
        assertNull(ui.game.board[1], "goat captured")
        assertEquals(1, ui.game.goatsCaptured)
        assertNull(ui.selected)
        assertEquals(Side.GOAT, ui.game.sideToMove)
    }

    @Test
    fun `tapping the selected piece again deselects it`() {
        val ui = GameInteraction.newGame().tap(12, 0, 0)
        assertNull(ui.selected)
        assertEquals(emptySet(), ui.targets)
    }

    @Test
    fun `tapping another movable piece switches the selection`() {
        val ui = GameInteraction.newGame().tap(12, 0, 4)
        assertEquals(4, ui.selected)
    }

    @Test
    fun `tapping a non-target point clears the selection without moving`() {
        val before = GameInteraction.newGame().tap(12, 0)
        val after = before.tap(18)
        assertNull(after.selected)
        assertEquals(before.game, after.game)
    }

    @Test
    fun `opponent pieces cannot be selected`() {
        val ui = GameInteraction.newGame().tap(12).tap(12)
        assertNull(ui.selected)
        assertEquals(Side.TIGER, ui.game.sideToMove)
    }

    @Test
    fun `taps are ignored once the game is over`() {
        val tigers = listOf(1, 5, 6, 24)
        val goats = (0 until 25).filter { it != 0 && it !in tigers }
        val game = GameState.create(tigers, goats, Side.GOAT, goatsInHand = 0, goatsCaptured = 0)
        val ui = GameUiState(game, Rules.status(game), null, emptySet())
        assertEquals(GameStatus.Won(Side.TIGER, WinReason.GOATS_BLOCKED), ui.status)
        assertSame(ui, ui.tap(0))
    }

    // --- Vs computer ---

    @Test
    fun `taps are ignored while it is the computer's turn`() {
        val ui = GameInteraction.newGame(aiSide = Side.GOAT)
        assertEquals(true, ui.isAiTurn)
        assertEquals(emptySet(), ui.targets, "no highlights for the computer's pieces")
        assertSame(ui, ui.tap(12))
    }

    @Test
    fun `computer move is applied and hands the turn to the player`() {
        val ui = GameInteraction.applyMove(GameInteraction.newGame(aiSide = Side.GOAT), Move.Place(12))
        assertEquals(Side.GOAT, ui.game.board[12])
        assertEquals(false, ui.isAiTurn)
        assertEquals(Side.GOAT, ui.aiSide)
        assertEquals(0, ui.tap(0).selected, "player can now select a tiger")
    }

    @Test
    fun `player move hands the turn to the computer`() {
        val ui = GameInteraction.newGame(aiSide = Side.TIGER).tap(12)
        assertEquals(true, ui.isAiTurn)
        assertEquals(emptySet(), ui.targets)
    }

    @Test
    fun `setup round-trips through navigation arguments`() {
        for (setup in listOf(
            GameSetup.TWO_PLAYER,
            GameSetup.vsComputer(Side.GOAT, Difficulty.EASY),
            GameSetup.vsComputer(Side.TIGER, Difficulty.HARD),
        )) {
            val args = setup.route.substringAfter('?').split('&').associate { it.substringBefore('=') to it.substringAfter('=') }
            assertEquals(setup, GameSetup.fromArgs(args[GameSetup.ARG_AI], args[GameSetup.ARG_DIFFICULTY]))
            assertEquals(null, args[GameSetup.ARG_RESUME], "new-game routes never resume")
        }
        assertEquals(Side.TIGER, GameSetup.vsComputer(Side.GOAT, Difficulty.EASY).aiSide)
        assertEquals(GameSetup.TWO_PLAYER, GameSetup.fromArgs(null, null))
    }
}
