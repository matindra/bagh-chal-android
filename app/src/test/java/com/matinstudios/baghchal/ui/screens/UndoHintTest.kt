package com.matinstudios.baghchal.ui.screens

import com.matinstudios.baghchal.data.Settings
import com.matinstudios.baghchal.engine.GameState
import com.matinstudios.baghchal.engine.GameStatus
import com.matinstudios.baghchal.engine.Move
import com.matinstudios.baghchal.engine.Rules
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.engine.ai.Difficulty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class UndoHintTest {

    private fun GameUiState.play(vararg moves: Move) = moves.fold(this) { ui, move -> GameInteraction.applyMove(ui, move) }

    private fun GameUiState.undo() = GameInteraction.undo(this)

    // --- Undo ---

    @Test
    fun `in 2-player each undo takes back one move, back to the start`() {
        val start = GameInteraction.newGame()
        assertFalse(start.canUndo)
        val placed = start.play(Move.Place(12))
        val stepped = placed.play(Move.Step(0, 5))
        assertTrue(stepped.canUndo)

        val once = stepped.undo()
        assertEquals(placed.game, once.game)
        assertEquals(Side.TIGER, once.game.sideToMove)
        val twice = once.undo()
        assertEquals(start.game, twice.game)
        assertFalse(twice.canUndo)
        assertSame(twice, twice.undo(), "nothing left to undo")
    }

    @Test
    fun `against the computer undo removes the player's move and the computer's reply`() {
        val start = GameInteraction.newGame(aiSide = Side.TIGER)
        val afterReply = start.play(Move.Place(12), Move.Step(0, 5), Move.Place(7), Move.Step(4, 9))
        val undone = afterReply.undo()
        assertEquals(start.play(Move.Place(12), Move.Step(0, 5)).game, undone.game)
        assertEquals(Side.GOAT, undone.game.sideToMove, "back to the player's turn")
        assertEquals(start.game, undone.undo().game)
    }

    @Test
    fun `undo while the computer is thinking takes back just the player's move`() {
        val start = GameInteraction.newGame(aiSide = Side.TIGER)
        val thinking = start.play(Move.Place(12)).copy(isAiThinking = true)
        assertTrue(thinking.isAiTurn)
        assertTrue(thinking.canUndo)
        val undone = thinking.undo()
        assertEquals(start.game, undone.game)
        assertFalse(undone.isAiThinking)
    }

    @Test
    fun `the computer's opening move cannot be undone`() {
        // The player is the tigers, so the computer's goats move first.
        val afterAiOpening = GameInteraction.newGame(aiSide = Side.GOAT).play(Move.Place(12))
        assertFalse(afterAiOpening.canUndo)
        val afterPlayer = afterAiOpening.play(Move.Step(0, 5), Move.Place(7))
        assertEquals(afterAiOpening.game, afterPlayer.undo().game)
        assertFalse(afterPlayer.undo().canUndo)
    }

    @Test
    fun `undo restores repetition counts exactly`() {
        // Tigers shuffle 0 -> 5 -> 0 while goats are placed; undo must not leave stale position counts behind.
        val ui = GameInteraction.newGame().play(Move.Place(12), Move.Step(0, 5), Move.Place(13), Move.Step(5, 0))
        val undone = ui.undo()
        assertEquals(Rules.apply(Rules.apply(Rules.apply(Rules.initialState(), Move.Place(12)), Move.Step(0, 5)), Move.Place(13)), undone.game)
    }

    @Test
    fun `undo snaps the board back without move feedback`() {
        val played = GameInteraction.newGame().play(Move.Place(12))
        val undone = played.undo()
        assertNull(undone.lastMove, "no move to animate")
        assertEquals(played.moveNumber + 1, undone.moveNumber, "the board still redraws")
        assertNull(feedbackFor(played, undone, Settings()), "no sound or haptic for an undo")
    }

    @Test
    fun `a finished game cannot be undone`() {
        val ui = GameInteraction.fromGame(
            GameState.create(
                tigers = listOf(12, 4, 20, 24),
                goats = listOf(13, 1, 2, 3, 5, 6, 8, 9, 10, 15, 16, 17, 19, 21, 22, 23),
                sideToMove = Side.TIGER,
                goatsInHand = 0,
                goatsCaptured = 4,
            ),
            aiSide = null,
        ).play(Move.Jump(12, 13, 14))
        assertIs<GameStatus.Won>(ui.status)
        assertFalse(ui.canUndo)
        assertFalse(ui.canHint)
    }

    @Test
    fun `undo is saved like any other position change`() {
        val placed = GameInteraction.newGame(aiSide = Side.TIGER).play(Move.Place(12), Move.Step(0, 5), Move.Place(7))
        val undone = placed.play(Move.Step(4, 9)).undo()
        val actions = persistenceActions(placed.play(Move.Step(4, 9)), undone, Difficulty.EASY)
        assertEquals(1, actions.size)
        assertEquals(undone.game, (actions.single() as PersistenceAction.Save).saved.game)
    }

    // --- Hint ---

    @Test
    fun `hint is offered on the player's turn only`() {
        val ui = GameInteraction.newGame(aiSide = Side.TIGER)
        assertTrue(ui.canHint)
        val aiTurn = ui.play(Move.Place(12))
        assertFalse(aiTurn.canHint)
        assertFalse(ui.copy(isHintLoading = true).canHint, "already working on one")
    }

    @Test
    fun `a hint is shown until the next move and survives selection`() {
        val ui = GameInteraction.newGame().play(Move.Place(12))
        val hinted = GameInteraction.showHint(ui.copy(isHintLoading = true), ui.game, Move.Step(0, 6))
        assertEquals(Move.Step(0, 6), hinted.hint)
        assertFalse(hinted.isHintLoading)
        assertFalse(hinted.canHint, "one hint at a time")

        val selected = GameInteraction.onPointTapped(hinted, 0)
        assertEquals(0, selected.selected)
        assertEquals(Move.Step(0, 6), selected.hint)

        assertNull(selected.play(Move.Step(0, 5)).hint)
        assertNull(hinted.undo().hint)
    }

    @Test
    fun `a hint for a position that has since changed is dropped`() {
        val ui = GameInteraction.newGame()
        val moved = ui.play(Move.Place(12)).copy(isHintLoading = true)
        val result = GameInteraction.showHint(moved, ui.game, Move.Place(6))
        assertNull(result.hint)
        assertFalse(result.isHintLoading)
    }
}
