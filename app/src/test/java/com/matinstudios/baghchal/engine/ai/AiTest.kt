package com.matinstudios.baghchal.engine.ai

import com.matinstudios.baghchal.engine.GameState
import com.matinstudios.baghchal.engine.GameStatus
import com.matinstudios.baghchal.engine.Move
import com.matinstudios.baghchal.engine.Rules
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.engine.TOTAL_GOATS
import com.matinstudios.baghchal.engine.WinReason
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AiTest {

    private val fast = SearchLimits(maxDepth = 3, timeBudgetMillis = 200)

    private fun bestMove(state: GameState, limits: SearchLimits = fast, seed: Int = 1): Move? =
        Search(Random(seed)).findBestMove(state, limits)

    @Test
    fun `returns a legal move in every tested position`() {
        for (state in randomPositions(150)) {
            val move = assertNotNull(bestMove(state), "no move for side ${state.sideToMove}")
            assertTrue(Rules.isLegal(state, move), "illegal $move")
        }
    }

    @Test
    fun `every difficulty returns a legal move`() {
        for (difficulty in Difficulty.entries) {
            for (state in randomPositions(4, seed = difficulty.ordinal)) {
                val move = assertNotNull(Search(Random(3)).findBestMove(state, difficulty.limits))
                assertTrue(Rules.isLegal(state, move), "$difficulty played illegal $move")
            }
        }
    }

    @Test
    fun `easy picks only legal moves across many seeds`() {
        val state = Rules.initialState()
        repeat(30) { seed ->
            val move = assertNotNull(bestMove(state, Difficulty.EASY.limits, seed))
            assertTrue(Rules.isLegal(state, move))
        }
    }

    @Test
    fun `respects its time budget`() {
        val budget = Difficulty.HARD.limits.timeBudgetMillis
        for (state in randomPositions(5, seed = 11)) {
            val start = System.nanoTime()
            bestMove(state, Difficulty.HARD.limits)
            val elapsedMs = (System.nanoTime() - start) / 1_000_000
            assertTrue(elapsedMs <= budget + 150, "took ${elapsedMs}ms with a ${budget}ms budget")
        }
    }

    @Test
    fun `respects a short time budget at unlimited depth`() {
        val limits = SearchLimits(maxDepth = 64, timeBudgetMillis = 100)
        for (state in randomPositions(5, seed = 12)) {
            val start = System.nanoTime()
            val move = assertNotNull(bestMove(state, limits))
            val elapsedMs = (System.nanoTime() - start) / 1_000_000
            assertTrue(elapsedMs <= 100 + 100, "took ${elapsedMs}ms with a 100ms budget")
            assertTrue(Rules.isLegal(state, move))
        }
    }

    @Test
    fun `stop request ends the search early with a legal move`() {
        val state = randomPositions(1, seed = 5).single()
        val start = System.nanoTime()
        val move = Search().findBestMove(state, Difficulty.HARD.limits, shouldStop = { true })
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        assertTrue(elapsedMs < 300, "took ${elapsedMs}ms after stop")
        assertTrue(Rules.isLegal(state, assertNotNull(move)))
    }

    @Test
    fun `cancelling the coroutine stops the AI`() = runBlocking {
        val state = randomPositions(1, seed = 6).single()
        val ai = AiPlayer(SearchLimits(maxDepth = 64, timeBudgetMillis = 10_000))
        val start = System.nanoTime()
        val job = async(Dispatchers.Default) { ai.chooseMove(state) }
        delay(100)
        job.cancel()
        assertFailsWith<CancellationException> { job.await() }
        job.join()
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        assertTrue(elapsedMs < 1_000, "took ${elapsedMs}ms to stop")
    }

    @Test
    fun `returns null when the game is over`() {
        val tigers = listOf(1, 5, 6, 24)
        val goats = (0 until 25).filter { it != 0 && it !in tigers }
        val state = GameState.create(tigers, goats, Side.GOAT, goatsInHand = 0, goatsCaptured = 0)
        assertNull(bestMove(state))
    }

    @Test
    fun `tiger takes the winning fifth capture`() {
        val goats = listOf(13, 1, 2, 3, 5, 6, 8, 9, 10, 15, 16, 17, 19, 21, 22, 23)
        val state = GameState.create(listOf(12, 4, 20, 24), goats, Side.TIGER, goatsInHand = 0, goatsCaptured = 4)
        // Several captures win here (12 over 13, 12 over 6, 4 over 9); any of them is correct.
        val next = Rules.apply(state, assertNotNull(bestMove(state)))
        assertEquals(GameStatus.Won(Side.TIGER, WinReason.CAPTURES), Rules.status(next))
    }

    @Test
    fun `goat protects a threatened goat when it can`() {
        // Tiger 0 threatens to jump goat 6 to 12; placing on 12 is the only defence.
        val state = GameState.create(
            tigers = listOf(0, 4, 20, 24),
            goats = listOf(6),
            sideToMove = Side.GOAT,
            goatsInHand = TOTAL_GOATS - 1,
            goatsCaptured = 0,
        )
        val move = bestMove(state, SearchLimits(maxDepth = 3, timeBudgetMillis = 1_000))
        val next = Rules.apply(state, assertNotNull(move))
        assertTrue(Rules.legalMoves(next).none { it is Move.Jump }, "goat left hanging after $move")
    }

    @Test
    fun `ai vs ai games only ever play legal moves`() {
        repeat(3) { game ->
            var state = Rules.initialState()
            val tiger = Search(Random(game))
            val goat = Search(Random(game + 100))
            val limits = SearchLimits(maxDepth = 2, timeBudgetMillis = 50, randomMargin = 30)
            var plies = 0
            while (Rules.status(state) == GameStatus.InProgress && plies < 300) {
                val ai = if (state.sideToMove == Side.TIGER) tiger else goat
                val move = assertNotNull(ai.findBestMove(state, limits))
                state = Rules.apply(state, move) // throws if illegal
                plies++
            }
            assertTrue(Rules.status(state) != GameStatus.InProgress, "game $game did not finish")
        }
    }
}
