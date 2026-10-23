package com.matinstudios.baghchal.engine.ai

import com.matinstudios.baghchal.engine.GOATS_TO_WIN
import com.matinstudios.baghchal.engine.GameState
import com.matinstudios.baghchal.engine.Move
import com.matinstudios.baghchal.engine.Rules
import com.matinstudios.baghchal.engine.Side
import kotlin.math.abs
import kotlin.random.Random

/**
 * Negamax with alpha-beta pruning, iterative deepening, a transposition table and
 * captures-first move ordering. Not thread-safe: use one instance per AI player.
 */
class Search(private val random: Random = Random.Default) {

    private val table = TranspositionTable()

    /**
     * Best move for the side to move in [state], or null if the game is over.
     *
     * Always returns within roughly [SearchLimits.timeBudgetMillis]; [shouldStop] is polled during the
     * search and ends it early (the best move from the last completed depth is returned).
     */
    fun findBestMove(state: GameState, limits: SearchLimits, shouldStop: () -> Boolean = { false }): Move? {
        val rootMoves = Rules.legalMoves(state)
        if (rootMoves.size <= 1) return rootMoves.firstOrNull()

        val start = System.nanoTime()
        val run = Run(state, start + limits.timeBudgetMillis * 1_000_000, shouldStop)
        var order = rootMoves.sortedBy { if (it is Move.Jump) 0 else 1 }
        var scored: List<Pair<Move, Int>>? = null

        for (depth in 1..limits.maxDepth) {
            val result = try {
                run.searchRoot(order, depth, exactScores = limits.randomMargin > 0)
            } catch (_: Abort) {
                break
            }
            scored = result
            order = result.sortedByDescending { it.second }.map { it.first }
            val best = result.maxOf { it.second }
            if (isDecisive(best)) break
            // A deeper iteration would almost certainly not finish in the remaining time.
            if (System.nanoTime() - start > limits.timeBudgetMillis * 500_000) break
        }

        if (scored == null) return order.first()
        val best = scored.maxOf { it.second }
        val candidates = scored.filter { it.second >= best - limits.randomMargin }.map { it.first }
        return if (limits.randomMargin > 0) candidates.random(random) else candidates.first()
    }

    /** Thrown to unwind the search when time is up or [findBestMove]'s `shouldStop` fires. */
    private object Abort : RuntimeException() {
        override fun fillInStackTrace(): Throwable = this
    }

    private inner class Run(state: GameState, private val deadline: Long, private val shouldStop: () -> Boolean) {
        private val board = SearchBoard(state)
        private val drawPlies = state.config.maxPliesWithoutCapture
        private val history: Set<Long> = state.positionCounts.keys
        private val path = LongArray(MAX_PLY + 1)
        private var nodes = 0L

        fun searchRoot(order: List<Move>, depth: Int, exactScores: Boolean): List<Pair<Move, Int>> {
            path[0] = board.key
            var alpha = -INFINITY
            return order.map { move ->
                val score = child(move, depth, ply = 0, alpha = if (exactScores) -INFINITY else alpha, beta = INFINITY)
                if (score > alpha) alpha = score
                move to score
            }
        }

        /** Score of [move] for the side making it, searched to `depth - 1`. */
        private fun child(move: Move, depth: Int, ply: Int, alpha: Int, beta: Int): Int {
            val previousPlies = board.pliesSinceCapture
            board.make(move)
            path[ply + 1] = board.key
            try {
                return -negamax(depth - 1, ply + 1, -beta, -alpha)
            } finally {
                board.unmake(move, previousPlies)
            }
        }

        private fun negamax(depth: Int, ply: Int, alphaIn: Int, beta: Int): Int {
            if ((++nodes and 1023L) == 0L && (System.nanoTime() >= deadline || shouldStop())) throw Abort

            if (board.goatsCaptured >= GOATS_TO_WIN) return winFor(Side.TIGER, ply)
            if (isRepetition(ply) || board.pliesSinceCapture >= drawPlies) return 0
            val moves = board.moves()
            if (moves.isEmpty()) return -(WIN - ply) // side to move is trapped / blocked and loses
            if (depth <= 0 || ply >= MAX_PLY) return quiescence(ply)

            var alpha = alphaIn
            var ttMove = TranspositionTable.NO_MOVE
            val slot = table.probe(board.key)
            if (slot >= 0) {
                ttMove = table.move(slot)
                if (table.depth(slot) >= depth) {
                    val score = fromTable(table.score(slot), ply)
                    when (table.bound(slot)) {
                        TranspositionTable.Bound.EXACT -> return score
                        TranspositionTable.Bound.LOWER -> if (score >= beta) return score
                        TranspositionTable.Bound.UPPER -> if (score <= alpha) return score
                    }
                }
            }

            val ordered = if (ttMove == TranspositionTable.NO_MOVE) moves else {
                moves.sortedBy { if (TranspositionTable.encode(it) == ttMove) 0 else 1 }
            }
            var best = -INFINITY
            var bestMove = TranspositionTable.NO_MOVE
            for (move in ordered) {
                val score = child(move, depth, ply, alpha, beta)
                if (score > best) {
                    best = score
                    bestMove = TranspositionTable.encode(move)
                }
                if (score > alpha) alpha = score
                if (alpha >= beta) break
            }

            val bound = when {
                best <= alphaIn -> TranspositionTable.Bound.UPPER
                best >= beta -> TranspositionTable.Bound.LOWER
                else -> TranspositionTable.Bound.EXACT
            }
            table.store(board.key, depth, toTable(best, ply), bound, bestMove)
            return best
        }

        /**
         * Leaf score for the side to move. Tigers may first resolve one capture so a goat left
         * hanging at the horizon is not scored as safe.
         */
        private fun quiescence(ply: Int): Int {
            val standPat = Evaluator.evaluate(board)
            if (board.side == Side.GOAT) return -standPat
            var best = standPat
            for (capture in board.captures()) {
                val previousPlies = board.pliesSinceCapture
                board.make(capture)
                val score = if (board.goatsCaptured >= GOATS_TO_WIN) WIN - (ply + 1) else Evaluator.evaluate(board)
                board.unmake(capture, previousPlies)
                if (score > best) best = score
            }
            return best
        }

        /** Any recurrence of an earlier position is scored as a draw. */
        private fun isRepetition(ply: Int): Boolean {
            val key = board.key
            if (key in history) return true
            for (i in ply - 2 downTo 0 step 2) if (path[i] == key) return true
            return false
        }

        private fun winFor(winner: Side, ply: Int) = if (board.side == winner) WIN - ply else -(WIN - ply)
    }

    private companion object {
        const val INFINITY = 1_000_000
        const val WIN = 100_000
        const val MAX_PLY = 128

        fun isDecisive(score: Int) = abs(score) >= WIN - MAX_PLY

        // Win scores are stored relative to the node so they stay correct at any ply.
        fun toTable(score: Int, ply: Int) = when {
            score >= WIN - MAX_PLY -> score + ply
            score <= -(WIN - MAX_PLY) -> score - ply
            else -> score
        }

        fun fromTable(score: Int, ply: Int) = when {
            score >= WIN - MAX_PLY -> score - ply
            score <= -(WIN - MAX_PLY) -> score + ply
            else -> score
        }
    }
}
