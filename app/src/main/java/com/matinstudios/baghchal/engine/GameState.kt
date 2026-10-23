package com.matinstudios.baghchal.engine

const val TOTAL_GOATS = 20
const val GOATS_TO_WIN = 5

enum class Side {
    TIGER, GOAT;

    val opponent: Side get() = if (this == TIGER) GOAT else TIGER
}

enum class Phase { PLACEMENT, MOVEMENT }

/** Bit layout of [GameState.positionKey], shared with the AI's incremental key. */
internal object PositionKey {
    const val EMPTY = 0
    const val GOAT = 1
    const val TIGER = 2
    const val SIDE_BIT = 1L shl 50
    private const val IN_HAND_SHIFT = 51
    const val IN_HAND_MASK = 0x1FL shl IN_HAND_SHIFT

    fun code(side: Side?): Int = when (side) {
        null -> EMPTY
        Side.GOAT -> GOAT
        Side.TIGER -> TIGER
    }

    fun cellBits(point: Int, code: Int): Long = code.toLong() shl (point * 2)
    fun inHandBits(goatsInHand: Int): Long = goatsInHand.toLong() shl IN_HAND_SHIFT
}

/**
 * Draw settings.
 *
 * @property repetitionsForDraw the game is drawn when the same position occurs this many times.
 * @property maxPliesWithoutCapture the game is drawn after this many plies (single turns) with no capture.
 */
data class RulesConfig(
    val repetitionsForDraw: Int = 3,
    val maxPliesWithoutCapture: Int = 100,
) {
    init {
        require(repetitionsForDraw >= 2) { "repetitionsForDraw must be >= 2" }
        require(maxPliesWithoutCapture >= 1) { "maxPliesWithoutCapture must be >= 1" }
    }
}

/**
 * Immutable game position plus the history needed for draw detection.
 *
 * [positionCounts] maps [positionKey] to how often that position has occurred since the last
 * irreversible move (placement or capture); earlier positions can never recur.
 * [moveCount] is the number of turns (by either side) played so far.
 */
data class GameState(
    val board: List<Side?>,
    val sideToMove: Side,
    val goatsInHand: Int,
    val goatsCaptured: Int,
    val pliesSinceCapture: Int,
    val positionCounts: Map<Long, Int>,
    val config: RulesConfig,
    val moveCount: Int = 0,
) {
    init {
        require(board.size == Board.POINTS) { "Board must have ${Board.POINTS} points" }
        require(goatsInHand >= 0 && goatsCaptured >= 0) { "Goat counts must be non-negative" }
        require(moveCount >= 0) { "moveCount must be non-negative" }
        require(goatsOnBoard + goatsInHand + goatsCaptured == TOTAL_GOATS) {
            "Goats on board + in hand + captured must equal $TOTAL_GOATS"
        }
    }

    val goatsOnBoard: Int get() = board.count { it == Side.GOAT }

    /** Placement lasts until every goat has left the hand. */
    val phase: Phase get() = if (goatsInHand > 0) Phase.PLACEMENT else Phase.MOVEMENT

    fun pointsOf(side: Side): List<Int> = board.indices.filter { board[it] == side }

    /**
     * Exact, collision-free encoding of the position: 2 bits per point, the side to move,
     * and goats in hand (captured goats follow from the other counts).
     */
    val positionKey: Long
        get() {
            var key = 0L
            for (point in 0 until Board.POINTS) {
                key = key or PositionKey.cellBits(point, PositionKey.code(board[point]))
            }
            if (sideToMove == Side.TIGER) key = key or PositionKey.SIDE_BIT
            return key or PositionKey.inHandBits(goatsInHand)
        }

    companion object {
        /** Builds a position from piece locations; its occurrence is recorded once in [positionCounts]. */
        fun create(
            tigers: Collection<Int>,
            goats: Collection<Int>,
            sideToMove: Side,
            goatsInHand: Int,
            goatsCaptured: Int = TOTAL_GOATS - goatsInHand - goats.size,
            pliesSinceCapture: Int = 0,
            config: RulesConfig = RulesConfig(),
            moveCount: Int = 0,
        ): GameState {
            require(tigers.intersect(goats.toSet()).isEmpty()) { "A point holds at most one piece" }
            val board = List(Board.POINTS) { point ->
                when (point) {
                    in tigers -> Side.TIGER
                    in goats -> Side.GOAT
                    else -> null
                }
            }
            val state = GameState(
                board = board,
                sideToMove = sideToMove,
                goatsInHand = goatsInHand,
                goatsCaptured = goatsCaptured,
                pliesSinceCapture = pliesSinceCapture,
                positionCounts = emptyMap(),
                config = config,
                moveCount = moveCount,
            )
            return state.copy(positionCounts = mapOf(state.positionKey to 1))
        }
    }
}
