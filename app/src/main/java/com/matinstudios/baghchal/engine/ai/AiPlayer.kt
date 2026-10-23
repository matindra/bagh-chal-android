package com.matinstudios.baghchal.engine.ai

import com.matinstudios.baghchal.engine.GameState
import com.matinstudios.baghchal.engine.Move
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlin.random.Random

/** Computer opponent. The search is CPU-bound: call from a background dispatcher. */
class AiPlayer(
    private val limits: SearchLimits,
    random: Random = Random.Default,
) {
    constructor(difficulty: Difficulty) : this(difficulty.limits)

    private val search = Search(random)

    /** Best move for the side to move, or null if the game is over. Stops early if the calling coroutine is cancelled. */
    suspend fun chooseMove(state: GameState): Move? {
        val job = currentCoroutineContext()[Job]
        return search.findBestMove(state, limits, shouldStop = { job?.isActive == false })
    }
}
