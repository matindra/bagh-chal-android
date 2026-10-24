package com.matinstudios.baghchal.ui.screens

import com.matinstudios.baghchal.data.Settings
import com.matinstudios.baghchal.engine.GameStatus
import com.matinstudios.baghchal.engine.Move
import com.matinstudios.baghchal.ui.sound.SoundEffect

enum class HapticCue { GOAT_PLACED, GOAT_CAPTURED }

/** Cues for one move, already filtered by the player's settings. */
data class Feedback(val haptic: HapticCue?, val sound: SoundEffect?)

/** Cues for the move that turned [previous] into [current] (moves by either side), or null. */
fun feedbackFor(previous: GameUiState?, current: GameUiState, settings: Settings): Feedback? {
    if (previous == null || current.moveNumber == previous.moveNumber) return null
    val move = current.lastMove ?: return null
    val haptic = when (move) {
        is Move.Place -> HapticCue.GOAT_PLACED
        is Move.Jump -> HapticCue.GOAT_CAPTURED
        is Move.Step -> null
    }.takeIf { settings.hapticsEnabled }
    val sound = soundFor(previous, current, move).takeIf { settings.soundEnabled }
    return if (haptic == null && sound == null) null else Feedback(haptic, sound)
}

/**
 * One sound per move, most significant first: the game-end sound (vs the computer: whether the
 * player won; 2-player: always the win sound), then a tiger becoming trapped, then the move itself.
 */
private fun soundFor(previous: GameUiState, current: GameUiState, move: Move): SoundEffect {
    val aiSide = current.aiSide
    when (val status = current.status) {
        is GameStatus.Won -> return if (aiSide == null || status.winner != aiSide) SoundEffect.PLAYER_WIN else SoundEffect.PLAYER_LOSE
        // A draw is neither a win nor a loss against the computer, so it just sounds like the move.
        is GameStatus.Drawn -> if (aiSide == null) return SoundEffect.PLAYER_WIN
        GameStatus.InProgress -> Unit
    }
    if ((current.trappedTigers - previous.trappedTigers).isNotEmpty()) return SoundEffect.GOAT_CAPTURE
    return when (move) {
        is Move.Place, is Move.Step -> SoundEffect.MOVE
        is Move.Jump -> SoundEffect.TIGER_CAPTURE
    }
}
