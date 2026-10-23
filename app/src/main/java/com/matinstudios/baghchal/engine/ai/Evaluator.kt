package com.matinstudios.baghchal.engine.ai

import com.matinstudios.baghchal.engine.Board
import com.matinstudios.baghchal.engine.PositionKey.EMPTY
import com.matinstudios.baghchal.engine.PositionKey.GOAT
import com.matinstudios.baghchal.engine.PositionKey.TIGER

/** Static evaluation, positive = good for tigers. */
internal object Evaluator {
    const val CAPTURED_GOAT = 100
    const val TIGER_MOBILITY = 3
    const val CAPTURABLE_GOAT = 25
    const val TRAPPED_TIGER = 25
    const val GOAT_ON_EDGE = 2

    private val EDGE = BooleanArray(Board.POINTS) {
        Board.row(it) == 0 || Board.row(it) == Board.SIZE - 1 || Board.col(it) == 0 || Board.col(it) == Board.SIZE - 1
    }

    fun evaluate(board: SearchBoard): Int {
        val cells = board.cells
        var mobility = 0
        var trapped = 0
        var capturable = 0 // bitmask of goats some tiger can capture right now
        var edgeGoats = 0
        for (point in 0 until Board.POINTS) {
            when (cells[point]) {
                GOAT -> if (EDGE[point]) edgeGoats++
                TIGER -> {
                    var moves = 0
                    for (to in Board.adjacency[point]) if (cells[to] == EMPTY) moves++
                    for (line in Board.jumps[point]) {
                        if (cells[line.over] == GOAT && cells[line.landing] == EMPTY) {
                            moves++
                            capturable = capturable or (1 shl line.over)
                        }
                    }
                    mobility += moves
                    if (moves == 0) trapped++
                }
            }
        }
        return CAPTURED_GOAT * board.goatsCaptured +
            TIGER_MOBILITY * mobility +
            CAPTURABLE_GOAT * Integer.bitCount(capturable) -
            TRAPPED_TIGER * trapped -
            GOAT_ON_EDGE * edgeGoats
    }
}
