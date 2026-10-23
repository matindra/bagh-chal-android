package com.matinstudios.baghchal.ui.screens

import com.matinstudios.baghchal.data.Settings
import com.matinstudios.baghchal.engine.Move
import com.matinstudios.baghchal.engine.Side
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GameFeedbackTest {

    private val all = Settings(soundEnabled = true, hapticsEnabled = true)

    private fun GameUiState.tap(vararg points: Int) =
        points.fold(this) { ui, point -> GameInteraction.onPointTapped(ui, point) }

    @Test
    fun `placing a goat gives placement feedback`() {
        val before = GameInteraction.newGame()
        val after = before.tap(12)
        assertEquals(Feedback(FeedbackEvent.GOAT_PLACED, haptic = true, sound = true), feedbackFor(before, after, all))
    }

    @Test
    fun `capturing gives capture feedback, also when the computer captures`() {
        val before = GameInteraction.newGame(aiSide = Side.TIGER).tap(1)
        val after = GameInteraction.applyMove(before, Move.Jump(0, 1, 2))
        assertEquals(FeedbackEvent.GOAT_CAPTURED, feedbackFor(before, after, all)?.event)
    }

    @Test
    fun `steps, selections and loading give no feedback`() {
        val placed = GameInteraction.newGame().tap(12)
        val stepped = GameInteraction.applyMove(placed, Move.Step(0, 5))
        assertNull(feedbackFor(placed, stepped, all), "tiger step")
        val selected = placed.tap(0)
        assertNull(feedbackFor(placed, selected, all), "selection only")
        assertNull(feedbackFor(null, GameInteraction.newGame(), all), "new game")
    }

    @Test
    fun `settings filter each channel`() {
        val before = GameInteraction.newGame()
        val after = before.tap(12)
        assertEquals(
            Feedback(FeedbackEvent.GOAT_PLACED, haptic = false, sound = true),
            feedbackFor(before, after, Settings(soundEnabled = true, hapticsEnabled = false)),
        )
        assertEquals(
            Feedback(FeedbackEvent.GOAT_PLACED, haptic = true, sound = false),
            feedbackFor(before, after, Settings(soundEnabled = false, hapticsEnabled = true)),
        )
        assertNull(feedbackFor(before, after, Settings(soundEnabled = false, hapticsEnabled = false)))
    }

    @Test
    fun `moves are numbered and survive selection changes`() {
        val placed = GameInteraction.newGame().tap(12)
        assertEquals(Move.Place(12), placed.lastMove)
        assertEquals(1, placed.moveNumber)
        val selected = placed.tap(0)
        assertEquals(0, selected.selected)
        assertEquals(Move.Place(12), selected.lastMove, "selecting must not reset the animation state")
        assertEquals(1, selected.moveNumber)
        val moved = selected.tap(5)
        assertEquals(Move.Step(0, 5), moved.lastMove)
        assertEquals(2, moved.moveNumber)
    }
}
