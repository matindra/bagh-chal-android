package com.matinstudios.baghchal.ui.screens

import com.matinstudios.baghchal.engine.GameState
import com.matinstudios.baghchal.engine.GameStatus
import com.matinstudios.baghchal.engine.Move
import com.matinstudios.baghchal.engine.Rules
import com.matinstudios.baghchal.engine.Side

/**
 * What the game screen shows.
 *
 * @property selected the piece the player has picked up, if any.
 * @property targets points the player can tap to complete a move: destinations of [selected],
 *   or every empty point while goats are being placed.
 * @property aiSide the computer's side, or null in 2-player games.
 * @property lastMove the most recent move, for animation; null for a new or resumed game.
 * @property moveNumber counts moves played on this screen, so each one is animated exactly once.
 * @property trappedTigers tigers with no legal move right now (shown with a lock badge).
 */
data class GameUiState(
    val game: GameState,
    val status: GameStatus,
    val selected: Int?,
    val targets: Set<Int>,
    val aiSide: Side? = null,
    val isAiThinking: Boolean = false,
    val lastMove: Move? = null,
    val moveNumber: Int = 0,
    val trappedTigers: Set<Int> = emptySet(),
) {
    val isAiTurn: Boolean get() = status == GameStatus.InProgress && game.sideToMove == aiSide
}

/** Tap-to-select / tap-to-move logic, kept pure so it can be unit-tested. */
object GameInteraction {

    fun newGame(aiSide: Side? = null): GameUiState = fromGame(Rules.initialState(), aiSide)

    /** Screen state for an existing (e.g. resumed) game. */
    fun fromGame(game: GameState, aiSide: Side?): GameUiState = uiState(game, selected = null, aiSide = aiSide)

    fun onPointTapped(ui: GameUiState, point: Int): GameUiState {
        if (ui.status != GameStatus.InProgress || ui.isAiTurn) return ui
        val moves = Rules.legalMoves(ui.game)
        val move = moves.firstOrNull { it.to == point && it.from == ui.selected }
        if (move != null) return applyMove(ui, move)

        // Tapping a movable piece selects it; tapping it again, or anything else, clears the selection.
        val canSelect = point != ui.selected && moves.any { it.from == point }
        val selected = point.takeIf { canSelect }
        return ui.copy(selected = selected, targets = targets(ui.game, ui.status, selected, ui.aiSide))
    }

    /** Plays [move] (from the player or the AI) and clears the selection. */
    fun applyMove(ui: GameUiState, move: Move): GameUiState =
        uiState(Rules.apply(ui.game, move), selected = null, aiSide = ui.aiSide)
            .copy(isAiThinking = ui.isAiThinking, lastMove = move, moveNumber = ui.moveNumber + 1)

    private fun uiState(game: GameState, selected: Int?, aiSide: Side?): GameUiState {
        val status = Rules.status(game)
        return GameUiState(
            game = game,
            status = status,
            selected = selected,
            targets = targets(game, status, selected, aiSide),
            aiSide = aiSide,
            trappedTigers = Rules.trappedTigers(game),
        )
    }

    private fun targets(game: GameState, status: GameStatus, selected: Int?, aiSide: Side?): Set<Int> {
        val humanToMove = status == GameStatus.InProgress && game.sideToMove != aiSide
        if (!humanToMove) return emptySet()
        return Rules.legalMoves(game).filter { it.from == selected }.map { it.to }.toSet()
    }

    private val Move.from: Int?
        get() = when (this) {
            is Move.Place -> null
            is Move.Step -> from
            is Move.Jump -> from
        }
}
