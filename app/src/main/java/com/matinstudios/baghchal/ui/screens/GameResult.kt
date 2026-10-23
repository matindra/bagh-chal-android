package com.matinstudios.baghchal.ui.screens

import com.matinstudios.baghchal.engine.GameStatus
import com.matinstudios.baghchal.engine.Side

/** A won game, as shown on the result screen. [setup] lets Play Again start the same kind of game. */
data class GameResult(val winner: Side, val moveCount: Int, val setup: GameSetup) {

    val route: String
        get() = "$ROUTE_BASE?$ARG_WINNER=${winner.name}&$ARG_MOVES=$moveCount" +
            "&${GameSetup.ARG_AI}=${setup.aiSide?.name ?: GameSetup.NO_AI}&${GameSetup.ARG_DIFFICULTY}=${setup.difficulty.name}"

    companion object {
        private const val ROUTE_BASE = "result"
        const val ARG_WINNER = "winner"
        const val ARG_MOVES = "moves"
        const val ROUTE_PATTERN = "$ROUTE_BASE?$ARG_WINNER={$ARG_WINNER}&$ARG_MOVES={$ARG_MOVES}" +
            "&${GameSetup.ARG_AI}={${GameSetup.ARG_AI}}&${GameSetup.ARG_DIFFICULTY}={${GameSetup.ARG_DIFFICULTY}}"

        fun fromArgs(winner: String?, moves: Int, ai: String?, difficulty: String?) = GameResult(
            winner = Side.entries.firstOrNull { it.name == winner } ?: Side.TIGER,
            moveCount = moves,
            setup = GameSetup.fromArgs(ai, difficulty),
        )
    }
}

/** The result to show when [current] is the move that won the game; draws stay on the game screen. */
fun resultFor(previous: GameUiState?, current: GameUiState, setup: GameSetup): GameResult? {
    val status = current.status
    if (previous?.status != GameStatus.InProgress || status !is GameStatus.Won) return null
    return GameResult(status.winner, current.game.moveCount, setup)
}
