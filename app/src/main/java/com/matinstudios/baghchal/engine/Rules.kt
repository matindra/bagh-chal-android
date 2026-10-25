package com.matinstudios.baghchal.engine

object Rules {

    fun initialState(config: RulesConfig = RulesConfig()): GameState = GameState.create(
        tigers = Board.CORNERS,
        goats = emptyList(),
        sideToMove = Side.GOAT,
        goatsInHand = TOTAL_GOATS,
        config = config,
    )

    fun status(state: GameState): GameStatus {
        if (state.goatsCaptured >= GOATS_TO_WIN) {
            return GameStatus.Won(Side.TIGER, WinReason.CAPTURES)
        }
        if (generateMoves(state).isEmpty()) {
            return when (state.sideToMove) {
                Side.TIGER -> GameStatus.Won(Side.GOAT, WinReason.TIGERS_TRAPPED)
                Side.GOAT -> GameStatus.Won(Side.TIGER, WinReason.GOATS_BLOCKED)
            }
        }
        return drawReason(state)?.let { GameStatus.Drawn(it) } ?: GameStatus.InProgress
    }

    /** All legal moves for the side to move; empty once the game is over. */
    fun legalMoves(state: GameState): List<Move> =
        if (state.goatsCaptured >= GOATS_TO_WIN || drawReason(state) != null) emptyList()
        else generateMoves(state)

    fun isLegal(state: GameState, move: Move): Boolean = move in legalMoves(state)

    /** Returns the position after [move]. Turn always passes, so a tiger makes at most one capture per turn. */
    fun apply(state: GameState, move: Move): GameState {
        require(isLegal(state, move)) { "Illegal move $move" }
        val board = state.board.toMutableList()
        var goatsInHand = state.goatsInHand
        var goatsCaptured = state.goatsCaptured
        when (move) {
            is Move.Place -> {
                board[move.to] = Side.GOAT
                goatsInHand--
            }
            is Move.Step -> {
                board[move.to] = board[move.from]
                board[move.from] = null
            }
            is Move.Jump -> {
                board[move.to] = Side.TIGER
                board[move.from] = null
                board[move.over] = null
                goatsCaptured++
            }
        }
        val next = state.copy(
            board = board,
            sideToMove = state.sideToMove.opponent,
            goatsInHand = goatsInHand,
            goatsCaptured = goatsCaptured,
            pliesSinceCapture = if (move is Move.Jump) 0 else state.pliesSinceCapture + 1,
            moveCount = state.moveCount + 1,
        )
        val key = next.positionKey
        // Placements and captures change goat counts, so no earlier position can recur.
        val history = if (move is Move.Step) state.positionCounts else emptyMap()
        return next.copy(positionCounts = history + (key to (history[key] ?: 0) + 1))
    }

    /**
     * Tigers that could not move right now even if it were their turn: no empty neighbour and no
     * capture. When every tiger is trapped on the tigers' turn, goats win.
     */
    fun trappedTigers(state: GameState): Set<Int> = state.pointsOf(Side.TIGER).filterTo(mutableSetOf()) { tiger ->
        Board.adjacency[tiger].none { state.board[it] == null } &&
            Board.jumps[tiger].none { state.board[it.over] == Side.GOAT && state.board[it.landing] == null }
    }

    /**
     * Goats that could not step right now: every neighbouring point is taken. Goats can't move at all
     * while placing, so this only means something in the movement phase.
     */
    fun trappedGoats(state: GameState): Set<Int> = state.pointsOf(Side.GOAT).filterTo(mutableSetOf()) { goat ->
        Board.adjacency[goat].none { state.board[it] == null }
    }

    private fun drawReason(state: GameState): DrawReason? = when {
        (state.positionCounts[state.positionKey] ?: 0) >= state.config.repetitionsForDraw -> DrawReason.REPETITION
        state.pliesSinceCapture >= state.config.maxPliesWithoutCapture -> DrawReason.MOVE_LIMIT
        else -> null
    }

    private fun generateMoves(state: GameState): List<Move> = when (state.sideToMove) {
        Side.GOAT -> goatMoves(state)
        Side.TIGER -> tigerMoves(state)
    }

    private fun goatMoves(state: GameState): List<Move> =
        if (state.phase == Phase.PLACEMENT) {
            (0 until Board.POINTS).filter { state.board[it] == null }.map { Move.Place(it) }
        } else {
            stepMoves(state, Side.GOAT)
        }

    private fun tigerMoves(state: GameState): List<Move> {
        val captures = state.pointsOf(Side.TIGER).flatMap { from ->
            Board.jumps[from]
                .filter { state.board[it.over] == Side.GOAT && state.board[it.landing] == null }
                .map { Move.Jump(from, it.over, it.landing) }
        }
        return captures + stepMoves(state, Side.TIGER)
    }

    private fun stepMoves(state: GameState, side: Side): List<Move> =
        state.pointsOf(side).flatMap { from ->
            Board.adjacency[from].filter { state.board[it] == null }.map { Move.Step(from, it) }
        }
}
