package com.matinstudios.baghchal.ui.screens

import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.engine.ai.Difficulty

/** How a game is played: [aiSide] is the computer's side, or null for local 2-player. */
data class GameSetup(val aiSide: Side?, val difficulty: Difficulty) {

    val route: String get() = "$ROUTE_BASE?$ARG_AI=${aiSide?.name ?: NO_AI}&$ARG_DIFFICULTY=${difficulty.name}"

    companion object {
        private const val ROUTE_BASE = "game"
        const val ARG_AI = "ai"
        const val ARG_DIFFICULTY = "difficulty"
        const val NO_AI = "NONE"
        const val ROUTE_PATTERN = "$ROUTE_BASE?$ARG_AI={$ARG_AI}&$ARG_DIFFICULTY={$ARG_DIFFICULTY}"

        val TWO_PLAYER = GameSetup(aiSide = null, difficulty = Difficulty.MEDIUM)

        fun vsComputer(humanSide: Side, difficulty: Difficulty) = GameSetup(humanSide.opponent, difficulty)

        /** Parses navigation arguments; unknown or missing values fall back to 2-player / medium. */
        fun fromArgs(ai: String?, difficulty: String?) = GameSetup(
            aiSide = Side.entries.firstOrNull { it.name == ai },
            difficulty = Difficulty.entries.firstOrNull { it.name == difficulty } ?: Difficulty.MEDIUM,
        )
    }
}
