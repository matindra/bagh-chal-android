package com.matinstudios.baghchal.ui.screens

import com.matinstudios.baghchal.data.Settings
import com.matinstudios.baghchal.engine.GameState
import com.matinstudios.baghchal.engine.Move
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.ui.sound.SoundEffect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GameFeedbackTest {

    private val all = Settings(soundEnabled = true, hapticsEnabled = true)

    private fun GameUiState.tap(vararg points: Int) =
        points.fold(this) { ui, point -> GameInteraction.onPointTapped(ui, point) }

    private fun GameUiState.play(move: Move) = GameInteraction.applyMove(this, move)

    private fun sound(before: GameUiState, move: Move) = feedbackFor(before, before.play(move), all)?.sound

    /** Goats to move; goat 7 -> 2 traps every tiger (goats win). */
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

    /** Tigers to move with 4 captured; tiger 12 jumps 13 -> 14 for the fifth. */
    private fun oneCaptureFromWin(aiSide: Side?) = GameInteraction.fromGame(
        GameState.create(
            tigers = listOf(12, 4, 20, 24),
            goats = listOf(13, 1, 2, 3, 5, 6, 8, 9, 10, 15, 16, 17, 19, 21, 22, 23),
            sideToMove = Side.TIGER,
            goatsInHand = 0,
            goatsCaptured = 4,
        ),
        aiSide,
    )

    // --- Sounds for moves ---

    @Test
    fun `placing a goat plays move and a light haptic`() {
        val before = GameInteraction.newGame()
        assertEquals(Feedback(HapticCue.GOAT_PLACED, SoundEffect.MOVE), feedbackFor(before, before.tap(12), all))
    }

    @Test
    fun `any step plays move, with no haptic`() {
        val placed = GameInteraction.newGame().tap(12)
        assertEquals(Feedback(haptic = null, sound = SoundEffect.MOVE), feedbackFor(placed, placed.play(Move.Step(0, 5)), all))
    }

    @Test
    fun `a capture plays tiger_capture, also when the computer captures`() {
        val before = GameInteraction.newGame(aiSide = Side.TIGER).tap(1)
        assertEquals(Feedback(HapticCue.GOAT_CAPTURED, SoundEffect.TIGER_CAPTURE), feedbackFor(before, before.play(Move.Jump(0, 1, 2)), all))
    }

    @Test
    fun `a tiger becoming trapped plays goat_capture instead of the move sound`() {
        // Tiger 0 is boxed in by 1, 5, 6 with 2 and 10 blocked; a goat placed on 12 removes its last jump.
        val before = GameInteraction.fromGame(
            GameState.create(listOf(0, 4, 20, 24), listOf(1, 2, 5, 6, 10), Side.GOAT, goatsInHand = 15),
            aiSide = null,
        )
        assertEquals(emptySet(), before.trappedTigers)
        assertEquals(SoundEffect.GOAT_CAPTURE, sound(before, Move.Place(12)))
    }

    @Test
    fun `an already trapped tiger does not replay goat_capture`() {
        val before = GameInteraction.fromGame(
            GameState.create(listOf(0, 4, 20, 24), listOf(1, 2, 5, 6, 10, 12), Side.GOAT, goatsInHand = 14),
            aiSide = null,
        )
        assertEquals(setOf(0), before.trappedTigers)
        assertEquals(SoundEffect.MOVE, sound(before, Move.Place(18)))
    }

    // --- Game end ---

    @Test
    fun `beating the computer plays player_win instead of the move sound`() {
        assertEquals(SoundEffect.PLAYER_WIN, sound(almostTrapped(aiSide = Side.TIGER), Move.Step(7, 2)), "goats beat computer tigers")
        assertEquals(SoundEffect.PLAYER_WIN, sound(oneCaptureFromWin(aiSide = Side.GOAT), Move.Jump(12, 13, 14)), "tigers beat computer goats")
    }

    @Test
    fun `losing to the computer plays player_lose`() {
        assertEquals(SoundEffect.PLAYER_LOSE, sound(almostTrapped(aiSide = Side.GOAT), Move.Step(7, 2)), "computer goats trap the player")
        assertEquals(SoundEffect.PLAYER_LOSE, sound(oneCaptureFromWin(aiSide = Side.TIGER), Move.Jump(12, 13, 14)), "computer tigers win")
    }

    @Test
    fun `two-player games always end with player_win`() {
        assertEquals(SoundEffect.PLAYER_WIN, sound(almostTrapped(aiSide = null), Move.Step(7, 2)))
        assertEquals(SoundEffect.PLAYER_WIN, sound(oneCaptureFromWin(aiSide = null), Move.Jump(12, 13, 14)))
    }

    @Test
    fun `draws play player_win in 2-player and just the move sound against the computer`() {
        fun nearMoveLimit(aiSide: Side?) = GameInteraction.fromGame(
            GameState.create(listOf(0, 4, 20, 24), listOf(12), Side.GOAT, goatsInHand = 19, pliesSinceCapture = 99),
            aiSide,
        )
        assertEquals(SoundEffect.PLAYER_WIN, sound(nearMoveLimit(aiSide = null), Move.Place(7)))
        assertEquals(SoundEffect.MOVE, sound(nearMoveLimit(aiSide = Side.TIGER), Move.Place(7)))
    }

    // --- Settings and non-moves ---

    @Test
    fun `settings filter each channel`() {
        val before = GameInteraction.newGame()
        val after = before.tap(12)
        assertEquals(
            Feedback(haptic = null, sound = SoundEffect.MOVE),
            feedbackFor(before, after, Settings(soundEnabled = true, hapticsEnabled = false)),
        )
        assertEquals(
            Feedback(HapticCue.GOAT_PLACED, sound = null),
            feedbackFor(before, after, Settings(soundEnabled = false, hapticsEnabled = true)),
        )
        assertNull(feedbackFor(before, after, Settings(soundEnabled = false, hapticsEnabled = false)))
        val stepped = after.play(Move.Step(0, 5))
        assertNull(feedbackFor(after, stepped, Settings(soundEnabled = false, hapticsEnabled = true)), "a step has no haptic")
    }

    @Test
    fun `selections and loading give no feedback`() {
        val placed = GameInteraction.newGame().tap(12)
        assertNull(feedbackFor(placed, placed.tap(0), all), "selection only")
        assertNull(feedbackFor(null, GameInteraction.newGame(), all), "new game")
        assertNull(feedbackFor(null, placed, all), "resumed game")
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
