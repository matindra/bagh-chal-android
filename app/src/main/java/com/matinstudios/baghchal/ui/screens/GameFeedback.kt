package com.matinstudios.baghchal.ui.screens

import com.matinstudios.baghchal.data.Settings
import com.matinstudios.baghchal.engine.Move

enum class FeedbackEvent { GOAT_PLACED, GOAT_CAPTURED }

/** A one-off cue for the screen to play; each channel is already filtered by the player's settings. */
data class Feedback(val event: FeedbackEvent, val haptic: Boolean, val sound: Boolean)

/** Cue for the move that turned [previous] into [current], if any (moves by either side). */
fun feedbackFor(previous: GameUiState?, current: GameUiState, settings: Settings): Feedback? {
    if (current.moveNumber == previous?.moveNumber) return null
    val event = when (current.lastMove) {
        is Move.Place -> FeedbackEvent.GOAT_PLACED
        is Move.Jump -> FeedbackEvent.GOAT_CAPTURED
        is Move.Step, null -> return null
    }
    if (!settings.hapticsEnabled && !settings.soundEnabled) return null
    return Feedback(event, haptic = settings.hapticsEnabled, sound = settings.soundEnabled)
}
