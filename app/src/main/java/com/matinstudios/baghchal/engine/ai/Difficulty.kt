package com.matinstudios.baghchal.engine.ai

/**
 * @property randomMargin the AI picks randomly among root moves scoring within this much of the best.
 */
data class SearchLimits(
    val maxDepth: Int,
    val timeBudgetMillis: Long,
    val randomMargin: Int = 0,
)

enum class Difficulty(val limits: SearchLimits) {
    EASY(SearchLimits(maxDepth = 2, timeBudgetMillis = 300, randomMargin = 60)),
    MEDIUM(SearchLimits(maxDepth = 4, timeBudgetMillis = 800)),
    HARD(SearchLimits(maxDepth = 64, timeBudgetMillis = 1500)),
}
