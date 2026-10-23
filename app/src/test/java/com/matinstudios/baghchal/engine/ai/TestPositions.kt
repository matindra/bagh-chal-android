package com.matinstudios.baghchal.engine.ai

import com.matinstudios.baghchal.engine.GameState
import com.matinstudios.baghchal.engine.GameStatus
import com.matinstudios.baghchal.engine.Rules
import kotlin.random.Random

/** Reachable in-progress positions from seeded random playouts, covering both phases and sides. */
fun randomPositions(count: Int, seed: Int = 42): List<GameState> {
    val random = Random(seed)
    val positions = ArrayList<GameState>()
    while (positions.size < count) {
        var state = Rules.initialState()
        val plies = random.nextInt(0, 80)
        for (i in 0 until plies) {
            val moves = Rules.legalMoves(state)
            if (moves.isEmpty()) break
            state = Rules.apply(state, moves.random(random))
        }
        if (Rules.status(state) == GameStatus.InProgress) positions += state
    }
    return positions
}
