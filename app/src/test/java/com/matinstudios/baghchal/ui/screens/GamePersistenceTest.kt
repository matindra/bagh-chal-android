package com.matinstudios.baghchal.ui.screens

import com.matinstudios.baghchal.data.Outcome
import com.matinstudios.baghchal.data.SavedGame
import com.matinstudios.baghchal.engine.GameState
import com.matinstudios.baghchal.engine.Move
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.engine.ai.Difficulty
import kotlin.test.Test
import kotlin.test.assertEquals

class GamePersistenceTest {

    private val difficulty = Difficulty.HARD

    /** Goats to move; goat 7 -> 2 traps every tiger (see RulesTest). */
    private fun almostTrapped(aiSide: Side?) = GameInteraction.fromGame(
        GameState.create(
            tigers = listOf(0, 4, 20, 24),
            goats = listOf(1, 3, 5, 6, 7, 8, 9, 10, 12, 14, 15, 16, 18, 19, 21, 22, 23),
            sideToMove = Side.GOAT,
            goatsInHand = 0,
            goatsCaptured = 3,
        ),
        aiSide,
    )

    @Test
    fun `a fresh game clears any old save`() {
        assertEquals(listOf(PersistenceAction.ClearSave), persistenceActions(null, GameInteraction.newGame(), difficulty))
    }

    @Test
    fun `every move of an in-progress game is saved`() {
        val before = GameInteraction.newGame(aiSide = Side.TIGER)
        val after = GameInteraction.applyMove(before, Move.Place(12))
        assertEquals(
            listOf(PersistenceAction.Save(SavedGame(after.game, Side.TIGER, difficulty))),
            persistenceActions(before, after, difficulty),
        )
    }

    @Test
    fun `selection and thinking changes are not saved`() {
        val ui = GameInteraction.newGame().let { GameInteraction.onPointTapped(it, 12) }
        val selected = GameInteraction.onPointTapped(ui, 0)
        assertEquals(emptyList(), persistenceActions(ui, selected, difficulty))
        assertEquals(emptyList(), persistenceActions(ui, ui.copy(isAiThinking = true), difficulty))
    }

    @Test
    fun `winning against the computer clears the save and records a win once`() {
        val before = almostTrapped(aiSide = Side.TIGER)
        val after = GameInteraction.applyMove(before, Move.Step(7, 2))
        assertEquals(
            listOf(PersistenceAction.ClearSave, PersistenceAction.RecordResult(Side.GOAT, difficulty, Outcome.WIN)),
            persistenceActions(before, after, difficulty),
        )
        assertEquals(emptyList(), persistenceActions(after, after.copy(isAiThinking = false), difficulty))
    }

    @Test
    fun `losing as tigers is recorded as a loss for tigers`() {
        val before = almostTrapped(aiSide = Side.GOAT)
        val after = GameInteraction.applyMove(before, Move.Step(7, 2))
        assertEquals(
            listOf(PersistenceAction.ClearSave, PersistenceAction.RecordResult(Side.TIGER, difficulty, Outcome.LOSS)),
            persistenceActions(before, after, difficulty),
        )
    }

    @Test
    fun `two-player games are never recorded`() {
        val before = almostTrapped(aiSide = null)
        val after = GameInteraction.applyMove(before, Move.Step(7, 2))
        assertEquals(listOf(PersistenceAction.ClearSave), persistenceActions(before, after, difficulty))
    }
}
