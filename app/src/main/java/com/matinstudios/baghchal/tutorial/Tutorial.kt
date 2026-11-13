package com.matinstudios.baghchal.tutorial

import com.matinstudios.baghchal.engine.Board
import com.matinstudios.baghchal.engine.GameState
import com.matinstudios.baghchal.engine.Move
import com.matinstudios.baghchal.engine.Rules
import com.matinstudios.baghchal.engine.Side

/** The guided first game's lessons, in order. The player is always the goats; the tigers follow a script. */
enum class Lesson {
    /** The board, the four tigers, the goats in hand. */
    MEET,

    /** Place the first goat; a tiger steps next to it. */
    FIRST_GOAT,

    /** The tiger threatens a jump; block its landing point. */
    BLOCK,

    /** Place a goat with an empty point behind it and watch it get eaten. */
    CARELESS,

    /** Pick any safe point; dangerous points are refused with the jump shown. */
    SAFE_SPOT,

    /** Movement phase: one step traps the last tiger and the goats win. */
    TRAP,
}

/** What the player must do to finish a step. */
sealed interface Goal {
    /** Read and press Next. */
    data object Next : Goal

    /** Make exactly this move. */
    data class Play(val move: Move) : Goal

    /** Place a goat on any point no tiger can jump over. */
    data object SafePlacement : Goal
}

/**
 * One step of the script.
 *
 * @property setup a fresh position for this step; null continues from the previous step's board.
 * @property reply the tigers' scripted answer to the player's move.
 * @property threat a tiger jump to point out while the player thinks.
 */
data class TutorialStep(
    val lesson: Lesson,
    val goal: Goal,
    val setup: GameState? = null,
    val reply: Move? = null,
    val threat: Move.Jump? = null,
)

enum class Stage {
    /** Waiting for the player's move. */
    INSTRUCTION,

    /** The player moved; the tiger answers next. */
    TIGER_TURN,

    /** The step is done: show what happened and a Next button. */
    DONE,
}

/** Why a tap was refused. */
enum class Nudge {
    TAP_GLOWING,
    TIGER_WOULD_EAT,
    TAP_GOAT,
}

data class TutorialState(
    val stepIndex: Int,
    val stage: Stage,
    val game: GameState,
    val selected: Int? = null,
    val nudge: Nudge? = null,
    /** The capture a refused placement would allow, drawn as an arrow. */
    val danger: Move.Jump? = null,
    val lastMove: Move? = null,
    /** Counts moves so the board animates each one once. */
    val moveNumber: Int = 0,
) {
    val step: TutorialStep get() = TutorialScript.STEPS[stepIndex]
    val isLastStep: Boolean get() = stepIndex == TutorialScript.STEPS.lastIndex

    /** Points that glow: where the player may move now. */
    val targets: Set<Int>
        get() {
            if (stage != Stage.INSTRUCTION) return emptySet()
            return when (val goal = step.goal) {
                Goal.Next -> emptySet()
                Goal.SafePlacement -> Tutorial.safePlacements(game)
                is Goal.Play -> when (val move = goal.move) {
                    is Move.Place -> setOf(move.to)
                    is Move.Step -> if (selected == move.from) setOf(move.to) else emptySet()
                    is Move.Jump -> emptySet()
                }
            }
        }

    /** The arrow on the board: a refused point's danger, the step's threat, or the move to make. */
    val arrow: Move?
        get() {
            if (stage != Stage.INSTRUCTION) return null
            danger?.let { return it }
            step.threat?.let { return it }
            val move = (step.goal as? Goal.Play)?.move
            return if (move is Move.Step && selected != move.from) move else null
        }

    val trappedTigers: Set<Int> get() = Rules.trappedTigers(game)
}

/** Pure transitions of the guided tutorial; the view model adds the pauses between them. */
object Tutorial {

    fun start(): TutorialState = enter(TutorialScript.STEPS.first(), 0, Rules.initialState(), moveNumber = 0)

    /** The player tapped [point]. */
    fun tap(state: TutorialState, point: Int): TutorialState {
        if (state.stage != Stage.INSTRUCTION) return state
        return when (val goal = state.step.goal) {
            Goal.Next -> state
            Goal.SafePlacement -> tapSafePlacement(state, point)
            is Goal.Play -> when (val move = goal.move) {
                is Move.Place -> if (point == move.to) play(state, move) else state.refuse(Nudge.TAP_GLOWING)
                is Move.Step -> when {
                    state.selected == move.from && point == move.to -> play(state, move)
                    point == move.from -> state.copy(selected = point, nudge = null, danger = null)
                    else -> state.refuse(if (state.selected == move.from) Nudge.TAP_GLOWING else Nudge.TAP_GOAT)
                }
                is Move.Jump -> state
            }
        }
    }

    /** The tigers answer the player's move. */
    fun tigerReply(state: TutorialState): TutorialState {
        val reply = state.step.reply
        if (state.stage != Stage.TIGER_TURN || reply == null) return state
        return state.copy(
            stage = Stage.DONE,
            game = Rules.apply(state.game, reply),
            lastMove = reply,
            moveNumber = state.moveNumber + 1,
        )
    }

    /** The next step, or null when the tutorial is over. */
    fun next(state: TutorialState): TutorialState? {
        if (state.stage != Stage.DONE || state.isLastStep) return null
        val index = state.stepIndex + 1
        val step = TutorialScript.STEPS[index]
        return enter(step, index, step.setup ?: state.game, state.moveNumber)
    }

    /** Empty points where a placed goat can't be jumped. */
    fun safePlacements(game: GameState): Set<Int> =
        (0 until Board.POINTS).filterTo(mutableSetOf()) { it.isEmptyIn(game) && captureOf(game, it) == null }

    /** The jump that would eat a goat placed on [point], if any. */
    fun captureOf(game: GameState, point: Int): Move.Jump? {
        val after = Rules.apply(game, Move.Place(point))
        return Rules.legalMoves(after).filterIsInstance<Move.Jump>().firstOrNull { it.over == point }
    }

    private fun tapSafePlacement(state: TutorialState, point: Int): TutorialState {
        if (!point.isEmptyIn(state.game)) return state.refuse(Nudge.TAP_GLOWING)
        val capture = captureOf(state.game, point)
            ?: return play(state, Move.Place(point))
        return state.copy(nudge = Nudge.TIGER_WOULD_EAT, danger = capture)
    }

    private fun play(state: TutorialState, move: Move): TutorialState = state.copy(
        stage = if (state.step.reply != null) Stage.TIGER_TURN else Stage.DONE,
        game = Rules.apply(state.game, move),
        selected = null,
        nudge = null,
        danger = null,
        lastMove = move,
        moveNumber = state.moveNumber + 1,
    )

    private fun TutorialState.refuse(nudge: Nudge) = copy(nudge = nudge, danger = null)

    private fun enter(step: TutorialStep, index: Int, game: GameState, moveNumber: Int) = TutorialState(
        stepIndex = index,
        stage = if (step.goal == Goal.Next) Stage.DONE else Stage.INSTRUCTION,
        game = game,
        moveNumber = moveNumber,
    )

    private fun Int.isEmptyIn(game: GameState) = game.board[this] == null
}

/**
 * The script. Points are `row * 5 + col`; tigers start on the corners 0, 4, 20, 24.
 * Unit tests check every scripted move is legal where it is played.
 */
object TutorialScript {
    val STEPS: List<TutorialStep> = listOf(
        TutorialStep(Lesson.MEET, Goal.Next),
        // Centre goat; the top-right tiger steps diagonally next to it.
        TutorialStep(Lesson.FIRST_GOAT, Goal.Play(Move.Place(12)), reply = Move.Step(4, 8)),
        // The tiger on 8 could jump 12 and land on 16: fill 16.
        TutorialStep(
            Lesson.BLOCK,
            Goal.Play(Move.Place(16)),
            reply = Move.Step(24, 19),
            threat = Move.Jump(8, 12, 16),
        ),
        // A goat on 7 has 6 empty behind it, so the tiger on 8 eats it.
        TutorialStep(Lesson.CARELESS, Goal.Play(Move.Place(7)), reply = Move.Jump(8, 7, 6)),
        // Fresh board: goats on the edge middles, so points beside the tigers are safe except the diagonals.
        TutorialStep(
            Lesson.SAFE_SPOT,
            Goal.SafePlacement,
            setup = GameState.create(
                tigers = Board.CORNERS,
                goats = listOf(2, 10, 14, 22),
                sideToMove = Side.GOAT,
                goatsInHand = 16,
            ),
        ),
        // Every goat placed; three tigers boxed in, and 17 -> 18 boxes in the last one.
        TutorialStep(
            Lesson.TRAP,
            Goal.Play(Move.Step(17, 18)),
            setup = GameState.create(
                tigers = Board.CORNERS,
                goats = listOf(1, 2, 3, 5, 6, 8, 9, 10, 12, 14, 15, 16, 17, 19, 21, 22, 23),
                sideToMove = Side.GOAT,
                goatsInHand = 0,
            ),
        ),
    )
}
