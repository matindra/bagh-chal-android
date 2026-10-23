package com.matinstudios.baghchal.engine

enum class WinReason {
    /** Tigers captured [GOATS_TO_WIN] goats. */
    CAPTURES,

    /** Tigers to move and no tiger has a legal move. */
    TIGERS_TRAPPED,

    /** Goats to move in the movement phase and no goat can move. */
    GOATS_BLOCKED,
}

enum class DrawReason { REPETITION, MOVE_LIMIT }

sealed interface GameStatus {
    data object InProgress : GameStatus
    data class Won(val winner: Side, val reason: WinReason) : GameStatus
    data class Drawn(val reason: DrawReason) : GameStatus
}
