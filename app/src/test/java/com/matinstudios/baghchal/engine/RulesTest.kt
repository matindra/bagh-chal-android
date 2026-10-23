package com.matinstudios.baghchal.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RulesTest {

    private fun GameState.play(vararg moves: Move): GameState =
        moves.fold(this) { state, move -> Rules.apply(state, move) }

    // --- Setup ---

    @Test
    fun `initial state has tigers on corners, 20 goats in hand, goats to move`() {
        val state = Rules.initialState()
        assertEquals(listOf(0, 4, 20, 24), state.pointsOf(Side.TIGER))
        assertEquals(emptyList(), state.pointsOf(Side.GOAT))
        assertEquals(20, state.goatsInHand)
        assertEquals(0, state.goatsCaptured)
        assertEquals(Side.GOAT, state.sideToMove)
        assertEquals(Phase.PLACEMENT, state.phase)
        assertEquals(GameStatus.InProgress, Rules.status(state))
    }

    // --- Placement phase ---

    @Test
    fun `goat can place on any empty point and nothing else at the start`() {
        val moves = Rules.legalMoves(Rules.initialState())
        assertEquals(21, moves.size)
        assertTrue(moves.all { it is Move.Place })
        assertEquals((0 until 25).toSet() - setOf(0, 4, 20, 24), moves.map { it.to }.toSet())
    }

    @Test
    fun `goats cannot step during placement even when goats are on the board`() {
        val state = GameState.create(
            tigers = listOf(0, 4, 20, 24),
            goats = listOf(12),
            sideToMove = Side.GOAT,
            goatsInHand = 19,
        )
        val moves = Rules.legalMoves(state)
        assertTrue(moves.all { it is Move.Place })
        assertFalse(Rules.isLegal(state, Move.Step(12, 7)))
        assertFalse(Rules.isLegal(state, Move.Place(12)), "cannot place on an occupied point")
        assertFalse(Rules.isLegal(state, Move.Place(0)), "cannot place on a tiger")
    }

    @Test
    fun `placing a goat takes it from hand and passes the turn`() {
        val next = Rules.initialState().play(Move.Place(12))
        assertEquals(Side.GOAT, next.board[12])
        assertEquals(19, next.goatsInHand)
        assertEquals(1, next.goatsOnBoard)
        assertEquals(Side.TIGER, next.sideToMove)
    }

    @Test
    fun `tigers can step and capture during placement`() {
        val state = Rules.initialState().play(Move.Place(1))
        val moves = Rules.legalMoves(state)
        assertTrue(Move.Step(0, 5) in moves)
        assertTrue(Move.Jump(0, 1, 2) in moves)
    }

    @Test
    fun `phase switches to movement after the 20th goat is placed`() {
        val goats = (0 until 25).filter { it !in listOf(0, 4, 20, 24, 12, 7) }.take(18)
        val state = GameState.create(
            tigers = listOf(0, 4, 20, 24),
            goats = goats,
            sideToMove = Side.GOAT,
            goatsInHand = 1,
            goatsCaptured = 1,
        )
        assertEquals(Phase.PLACEMENT, state.phase)
        val placed = Rules.apply(state, Move.Place(12))
        assertEquals(0, placed.goatsInHand)
        assertEquals(Phase.MOVEMENT, placed.phase)

        val tigerMove = Rules.legalMoves(placed).first()
        val goatTurn = Rules.apply(placed, tigerMove)
        val goatMoves = Rules.legalMoves(goatTurn)
        assertTrue(goatMoves.isNotEmpty())
        assertTrue(goatMoves.all { it is Move.Step })
    }

    @Test
    fun `goats step only to adjacent empty points in movement phase`() {
        val state = GameState.create(
            tigers = listOf(0, 1, 2, 3),
            goats = (10 until 25).toList() + 9,
            sideToMove = Side.GOAT,
            goatsInHand = 0,
            goatsCaptured = 4,
        )
        val moves = Rules.legalMoves(state)
        assertTrue(moves.all { it is Move.Step })
        assertEquals(setOf(Move.Step(9, 4), Move.Step(9, 8)), moves.filter { (it as Move.Step).from == 9 }.toSet())
        assertEquals(setOf(Move.Step(10, 5), Move.Step(10, 6)), moves.filter { (it as Move.Step).from == 10 }.toSet())
        assertEquals(setOf(Move.Step(11, 6)), moves.filter { (it as Move.Step).from == 11 }.toSet(), "11 has no diagonals")
    }

    // --- Captures ---

    private fun tigerToMove(tiger: Int, goats: List<Int>, otherTigers: List<Int> = listOf(4, 20, 24)) =
        GameState.create(
            tigers = listOf(tiger) + otherTigers,
            goats = goats,
            sideToMove = Side.TIGER,
            goatsInHand = TOTAL_GOATS - goats.size,
            goatsCaptured = 0,
        )

    @Test
    fun `tiger captures in a straight line onto an empty point`() {
        val state = tigerToMove(12, goats = listOf(7, 13, 18))
        val jumps = Rules.legalMoves(state).filterIsInstance<Move.Jump>().toSet()
        // Over 18 would land on the tiger at 24, so it is not a capture.
        assertEquals(setOf(Move.Jump(12, 7, 2), Move.Jump(12, 13, 14)), jumps)
    }

    @Test
    fun `capture is illegal when the landing point is occupied`() {
        val state = tigerToMove(12, goats = listOf(7, 2))
        assertFalse(Rules.isLegal(state, Move.Jump(12, 7, 2)))
        val tigerBlocked = tigerToMove(12, goats = listOf(18))
        assertFalse(Rules.isLegal(tigerBlocked, Move.Jump(12, 18, 24)), "tiger on landing point")
    }

    @Test
    fun `tiger cannot jump over a tiger or an empty point`() {
        val state = tigerToMove(0, goats = emptyList(), otherTigers = listOf(1, 20, 24))
        assertFalse(Rules.isLegal(state, Move.Jump(0, 1, 2)))
        assertFalse(Rules.isLegal(state, Move.Jump(0, 5, 10)))
    }

    @Test
    fun `no diagonal capture from a point without diagonals`() {
        // 7 is odd parity: 7 -> over 13 -> 19 would be diagonal and does not exist.
        val state = tigerToMove(7, goats = listOf(13, 11))
        val jumps = Rules.legalMoves(state).filterIsInstance<Move.Jump>()
        assertTrue(jumps.none { it.from == 7 && it.over in listOf(11, 13) })
    }

    @Test
    fun `no capture across the board edge`() {
        val state = tigerToMove(9, goats = listOf(8))
        assertTrue(Move.Jump(9, 8, 7) in Rules.legalMoves(state))
        val edge = tigerToMove(5, goats = listOf(0, 10))
        assertTrue(Rules.legalMoves(edge).filterIsInstance<Move.Jump>().none { it.from == 5 && it.over == 0 })
    }

    @Test
    fun `capture removes the goat, moves the tiger and passes the turn`() {
        val state = tigerToMove(12, goats = listOf(13))
        val next = Rules.apply(state, Move.Jump(12, 13, 14))
        assertEquals(Side.TIGER, next.board[14])
        assertEquals(null, next.board[12])
        assertEquals(null, next.board[13])
        assertEquals(1, next.goatsCaptured)
        assertEquals(0, next.pliesSinceCapture)
        assertEquals(Side.GOAT, next.sideToMove)
    }

    @Test
    fun `only one capture per turn`() {
        // After 0 jumps over 1 to 2, a second jump over 7 to 12 would be possible — but the turn passes.
        val state = tigerToMove(0, goats = listOf(1, 7))
        val next = Rules.apply(state, Move.Jump(0, 1, 2))
        assertEquals(Side.GOAT, next.sideToMove)
        assertEquals(Side.GOAT, next.board[7])
        assertEquals(1, next.goatsCaptured)
        assertTrue(Rules.legalMoves(next).none { it is Move.Jump })
    }

    @Test
    fun `capturing is optional`() {
        val state = tigerToMove(12, goats = listOf(13))
        val moves = Rules.legalMoves(state)
        assertTrue(Move.Jump(12, 13, 14) in moves)
        assertTrue(Move.Step(12, 11) in moves)
        Rules.apply(state, Move.Step(12, 11))
    }

    @Test
    fun `illegal moves are rejected`() {
        val state = Rules.initialState()
        assertFailsWith<IllegalArgumentException> { Rules.apply(state, Move.Step(0, 1)) }
        assertFailsWith<IllegalArgumentException> { Rules.apply(state, Move.Place(0)) }
    }

    // --- Wins ---

    @Test
    fun `tigers win on the fifth capture`() {
        val goats = listOf(13) + listOf(1, 2, 3, 5, 6, 8, 9, 10, 15, 16, 17, 19, 21, 22, 23)
        val state = GameState.create(
            tigers = listOf(12, 4, 20, 24),
            goats = goats,
            sideToMove = Side.TIGER,
            goatsInHand = 0,
            goatsCaptured = 4,
        )
        assertEquals(GameStatus.InProgress, Rules.status(state))
        val next = Rules.apply(state, Move.Jump(12, 13, 14))
        assertEquals(GOATS_TO_WIN, next.goatsCaptured)
        assertEquals(GameStatus.Won(Side.TIGER, WinReason.CAPTURES), Rules.status(next))
        assertTrue(Rules.legalMoves(next).isEmpty())
    }

    @Test
    fun `goats win when every tiger is trapped`() {
        // Goat 7 -> 2 fills the last gap (2 is both tiger 0's and tiger 4's capture landing).
        val goats = listOf(1, 3, 5, 6, 7, 8, 9, 10, 12, 14, 15, 16, 18, 19, 21, 22, 23)
        val state = GameState.create(
            tigers = listOf(0, 4, 20, 24),
            goats = goats,
            sideToMove = Side.GOAT,
            goatsInHand = 0,
            goatsCaptured = 3,
        )
        assertEquals(GameStatus.InProgress, Rules.status(state))
        val next = Rules.apply(state, Move.Step(7, 2))
        assertEquals(GameStatus.Won(Side.GOAT, WinReason.TIGERS_TRAPPED), Rules.status(next))
        assertTrue(Rules.legalMoves(next).isEmpty())
    }

    @Test
    fun `tigers win when goats cannot move in movement phase`() {
        val tigers = listOf(1, 5, 6, 24)
        val goats = (0 until 25).filter { it != 0 && it !in tigers }
        val state = GameState.create(
            tigers = tigers,
            goats = goats,
            sideToMove = Side.GOAT,
            goatsInHand = 0,
            goatsCaptured = 0,
        )
        assertEquals(GameStatus.Won(Side.TIGER, WinReason.GOATS_BLOCKED), Rules.status(state))
    }

    @Test
    fun `goats are never blocked during placement`() {
        val tigers = listOf(1, 5, 6, 24)
        val goats = (0 until 25).filter { it != 0 && it != 23 && it !in tigers }
        val state = GameState.create(
            tigers = tigers,
            goats = goats,
            sideToMove = Side.GOAT,
            goatsInHand = 1,
            goatsCaptured = 0,
        )
        assertEquals(GameStatus.InProgress, Rules.status(state))
        assertEquals(setOf(Move.Place(0), Move.Place(23)), Rules.legalMoves(state).toSet())
    }

    // --- Move count ---

    @Test
    fun `move count starts at zero and counts every turn`() {
        val state = Rules.initialState()
        assertEquals(0, state.moveCount)
        val after = state.play(Move.Place(1), Move.Jump(0, 1, 2), Move.Place(12))
        assertEquals(3, after.moveCount)
    }

    // --- Trapped tigers ---

    @Test
    fun `no tiger is trapped at the start`() {
        assertEquals(emptySet(), Rules.trappedTigers(Rules.initialState()))
    }

    @Test
    fun `a tiger with no step and no capture is trapped, regardless of whose turn it is`() {
        // Tiger 0: neighbours 1, 5, 6 are goats and every landing (2, 10, 12) is occupied.
        val goats = listOf(1, 2, 5, 6, 10, 12)
        for (side in Side.entries) {
            val state = GameState.create(listOf(0, 4, 20, 24), goats, side, goatsInHand = TOTAL_GOATS - goats.size)
            assertEquals(setOf(0), Rules.trappedTigers(state), "side to move $side")
        }
    }

    @Test
    fun `a tiger that can only capture is not trapped`() {
        // Tiger 0 is surrounded but can jump 1 -> 2.
        val goats = listOf(1, 5, 6, 10, 12)
        val state = GameState.create(listOf(0, 4, 20, 24), goats, Side.TIGER, goatsInHand = TOTAL_GOATS - goats.size)
        assertEquals(emptySet(), Rules.trappedTigers(state))
    }

    @Test
    fun `all four tigers are trapped when goats win by trapping`() {
        val goats = listOf(1, 3, 5, 6, 7, 8, 9, 10, 12, 14, 15, 16, 18, 19, 21, 22, 23)
        val state = GameState.create(listOf(0, 4, 20, 24), goats, Side.GOAT, goatsInHand = 0, goatsCaptured = 3)
        assertEquals(setOf(20, 24), Rules.trappedTigers(state), "tigers 0 and 4 can still jump into 2")
        val next = Rules.apply(state, Move.Step(7, 2))
        assertEquals(setOf(0, 4, 20, 24), Rules.trappedTigers(next))
    }

    // --- Draws ---

    /** Tigers on the top row, goats packed below; tiger 0<->5 and goat 9<->4 can shuffle forever. */
    private fun shuffleState(config: RulesConfig = RulesConfig()) = GameState.create(
        tigers = listOf(0, 1, 2, 3),
        goats = (10 until 25).toList() + 9,
        sideToMove = Side.TIGER,
        goatsInHand = 0,
        goatsCaptured = 4,
        config = config,
    )

    private val shuffleCycle = arrayOf(
        Move.Step(0, 5), Move.Step(9, 4), Move.Step(5, 0), Move.Step(4, 9),
    )

    @Test
    fun `threefold repetition is a draw`() {
        val once = shuffleState().play(*shuffleCycle)
        assertEquals(2, once.positionCounts[once.positionKey])
        assertEquals(GameStatus.InProgress, Rules.status(once))

        val twice = once.play(*shuffleCycle)
        assertEquals(3, twice.positionCounts[twice.positionKey])
        assertEquals(GameStatus.Drawn(DrawReason.REPETITION), Rules.status(twice))
        assertTrue(Rules.legalMoves(twice).isEmpty())
    }

    @Test
    fun `repetition count is configurable`() {
        val state = shuffleState(RulesConfig(repetitionsForDraw = 2)).play(*shuffleCycle)
        assertEquals(GameStatus.Drawn(DrawReason.REPETITION), Rules.status(state))
    }

    @Test
    fun `same pieces with a different side to move is a different position`() {
        val state = shuffleState()
        assertTrue(state.positionKey != state.copy(sideToMove = Side.GOAT).positionKey)
    }

    @Test
    fun `move limit without capture is a draw`() {
        val config = RulesConfig(repetitionsForDraw = 10, maxPliesWithoutCapture = 6)
        val state = shuffleState(config).play(*shuffleCycle, Move.Step(0, 5))
        assertEquals(5, state.pliesSinceCapture)
        assertEquals(GameStatus.InProgress, Rules.status(state))
        val limit = state.play(Move.Step(9, 4))
        assertEquals(GameStatus.Drawn(DrawReason.MOVE_LIMIT), Rules.status(limit))
    }

    @Test
    fun `a capture resets the move-limit counter and repetition history`() {
        val state = tigerToMove(12, goats = listOf(13)).copy(pliesSinceCapture = 50)
        val next = Rules.apply(state, Move.Jump(12, 13, 14))
        assertEquals(0, next.pliesSinceCapture)
        assertEquals(mapOf(next.positionKey to 1), next.positionCounts)
    }
}
