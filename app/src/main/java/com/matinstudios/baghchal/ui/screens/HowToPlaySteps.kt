package com.matinstudios.baghchal.ui.screens

import androidx.annotation.StringRes
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.engine.GameState
import com.matinstudios.baghchal.engine.Move
import com.matinstudios.baghchal.engine.Rules
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.engine.TOTAL_GOATS

/**
 * A rules example: pieces before [move], kept sparse for clarity (not necessarily a reachable game).
 * The screen draws the position after the move, with the origin faded and an arrow.
 */
data class RuleIllustration(val tigers: Set<Int>, val goats: Set<Int>, val move: Move) {

    val tigersAfter: Set<Int>
        get() = when (move) {
            is Move.Place -> tigers
            is Move.Step -> if (move.from in tigers) tigers - move.from + move.to else tigers
            is Move.Jump -> tigers - move.from + move.to
        }

    val goatsAfter: Set<Int>
        get() = when (move) {
            is Move.Place -> goats + move.to
            is Move.Step -> if (move.from in goats) goats - move.from + move.to else goats
            is Move.Jump -> goats - move.over
        }

    /** Tigers shown with a lock badge after the move. */
    val trappedAfter: Set<Int>
        get() = Rules.trappedTigers(
            GameState.create(tigersAfter, goatsAfter, Side.TIGER, goatsInHand = TOTAL_GOATS - goatsAfter.size, goatsCaptured = 0),
        )
}

data class HowToPlayStep(
    @get:StringRes val title: Int,
    @get:StringRes val body: Int,
    @get:StringRes val illustrationDescription: Int,
    val illustration: RuleIllustration,
)

private val CORNERS = setOf(0, 4, 20, 24)

val HOW_TO_PLAY_STEPS: List<HowToPlayStep> = listOf(
    HowToPlayStep(
        R.string.rules_place_title,
        R.string.rules_place_body,
        R.string.rules_place_picture,
        RuleIllustration(tigers = CORNERS, goats = setOf(6, 18), move = Move.Place(12)),
    ),
    HowToPlayStep(
        R.string.rules_goat_move_title,
        R.string.rules_goat_move_body,
        R.string.rules_goat_move_picture,
        RuleIllustration(tigers = CORNERS, goats = setOf(6, 7, 8, 16, 18), move = Move.Step(7, 12)),
    ),
    HowToPlayStep(
        R.string.rules_tiger_step_title,
        R.string.rules_tiger_step_body,
        R.string.rules_tiger_step_picture,
        RuleIllustration(tigers = CORNERS, goats = setOf(2, 12, 16), move = Move.Step(0, 6)),
    ),
    HowToPlayStep(
        R.string.rules_capture_title,
        R.string.rules_capture_body,
        R.string.rules_capture_picture,
        RuleIllustration(tigers = setOf(6, 4, 20, 24), goats = setOf(7, 12, 16), move = Move.Jump(6, 7, 8)),
    ),
    HowToPlayStep(
        R.string.rules_trap_title,
        R.string.rules_trap_body,
        R.string.rules_trap_picture,
        RuleIllustration(tigers = CORNERS, goats = setOf(1, 2, 5, 6, 10, 13), move = Move.Step(13, 12)),
    ),
)
