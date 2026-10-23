package com.matinstudios.baghchal.engine.ai

import com.matinstudios.baghchal.engine.Board
import com.matinstudios.baghchal.engine.GameState
import com.matinstudios.baghchal.engine.Move
import com.matinstudios.baghchal.engine.PositionKey
import com.matinstudios.baghchal.engine.PositionKey.EMPTY
import com.matinstudios.baghchal.engine.PositionKey.GOAT
import com.matinstudios.baghchal.engine.PositionKey.TIGER
import com.matinstudios.baghchal.engine.Side

/**
 * Mutable copy of a [GameState] for search: make/unmake instead of allocating new states, and
 * [key] kept equal to [GameState.positionKey] incrementally. Move generation mirrors [com.matinstudios.baghchal.engine.Rules].
 */
internal class SearchBoard(state: GameState) {
    val cells = IntArray(Board.POINTS) { PositionKey.code(state.board[it]) }
    var side: Side = state.sideToMove
        private set
    var goatsInHand: Int = state.goatsInHand
        private set
    var goatsCaptured: Int = state.goatsCaptured
        private set
    var pliesSinceCapture: Int = state.pliesSinceCapture
        private set
    var key: Long = state.positionKey
        private set

    /** Legal moves for [side], captures first. */
    fun moves(): List<Move> = when {
        side == Side.TIGER -> captures() + steps(TIGER)
        goatsInHand > 0 -> (0 until Board.POINTS).filter { cells[it] == EMPTY }.map { Move.Place(it) }
        else -> steps(GOAT)
    }

    fun captures(): List<Move.Jump> {
        val result = ArrayList<Move.Jump>()
        for (from in 0 until Board.POINTS) {
            if (cells[from] != TIGER) continue
            for (line in Board.jumps[from]) {
                if (cells[line.over] == GOAT && cells[line.landing] == EMPTY) {
                    result += Move.Jump(from, line.over, line.landing)
                }
            }
        }
        return result
    }

    private fun steps(piece: Int): List<Move> {
        val result = ArrayList<Move>()
        for (from in 0 until Board.POINTS) {
            if (cells[from] != piece) continue
            for (to in Board.adjacency[from]) {
                if (cells[to] == EMPTY) result += Move.Step(from, to)
            }
        }
        return result
    }

    fun make(move: Move) {
        when (move) {
            is Move.Place -> {
                set(move.to, GOAT)
                setGoatsInHand(goatsInHand - 1)
            }
            is Move.Step -> {
                val piece = cells[move.from]
                set(move.from, EMPTY)
                set(move.to, piece)
            }
            is Move.Jump -> {
                set(move.from, EMPTY)
                set(move.over, EMPTY)
                set(move.to, TIGER)
                goatsCaptured++
            }
        }
        pliesSinceCapture = if (move is Move.Jump) 0 else pliesSinceCapture + 1
        flipSide()
    }

    /** Reverts [make]; [previousPlies] is [pliesSinceCapture] from before the move. */
    fun unmake(move: Move, previousPlies: Int) {
        flipSide()
        when (move) {
            is Move.Place -> {
                set(move.to, EMPTY)
                setGoatsInHand(goatsInHand + 1)
            }
            is Move.Step -> {
                val piece = cells[move.to]
                set(move.to, EMPTY)
                set(move.from, piece)
            }
            is Move.Jump -> {
                set(move.to, EMPTY)
                set(move.over, GOAT)
                set(move.from, TIGER)
                goatsCaptured--
            }
        }
        pliesSinceCapture = previousPlies
    }

    private fun set(point: Int, code: Int) {
        key = key xor PositionKey.cellBits(point, cells[point]) xor PositionKey.cellBits(point, code)
        cells[point] = code
    }

    private fun setGoatsInHand(count: Int) {
        key = (key and PositionKey.IN_HAND_MASK.inv()) or PositionKey.inHandBits(count)
        goatsInHand = count
    }

    private fun flipSide() {
        side = side.opponent
        key = key xor PositionKey.SIDE_BIT
    }
}
