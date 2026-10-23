package com.matinstudios.baghchal.data

import com.matinstudios.baghchal.engine.GameState
import com.matinstudios.baghchal.engine.RulesConfig
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.engine.ai.Difficulty

/** An in-progress game and how it is being played ([aiSide] null = 2-player). */
data class SavedGame(val game: GameState, val aiSide: Side?, val difficulty: Difficulty)

/**
 * Versioned, pipe-separated text encoding of a [SavedGame]:
 * `v2|board|side|inHand|captured|plies|repetitions|maxPlies|ai|difficulty|key:count,...|moveCount`
 * where board is 25 chars of `.`, `G`, `T`. Kept dependency-free on purpose.
 * v1 (no move count) predates release and is no longer read.
 */
object SavedGameCodec {
    private const val VERSION = "v2"
    private const val FIELD = "|"
    private const val NO_AI = "-"

    fun encode(saved: SavedGame): String {
        val game = saved.game
        return listOf(
            VERSION,
            game.board.joinToString("") { it.toChar().toString() },
            game.sideToMove.toChar().toString(),
            game.goatsInHand.toString(),
            game.goatsCaptured.toString(),
            game.pliesSinceCapture.toString(),
            game.config.repetitionsForDraw.toString(),
            game.config.maxPliesWithoutCapture.toString(),
            saved.aiSide?.toChar()?.toString() ?: NO_AI,
            saved.difficulty.name,
            game.positionCounts.entries.joinToString(",") { "${it.key}:${it.value}" },
            game.moveCount.toString(),
        ).joinToString(FIELD)
    }

    /** Returns null for anything unreadable (other versions, corrupt or inconsistent data). */
    fun decode(text: String): SavedGame? = runCatching {
        val fields = text.split(FIELD)
        require(fields.size == 12 && fields[0] == VERSION)
        val game = GameState(
            board = fields[1].map { sideOf(it) },
            sideToMove = requireNotNull(sideOf(fields[2].single())),
            goatsInHand = fields[3].toInt(),
            goatsCaptured = fields[4].toInt(),
            pliesSinceCapture = fields[5].toInt(),
            positionCounts = fields[10].split(",").filter { it.isNotEmpty() }.associate { entry ->
                val (key, count) = entry.split(":")
                key.toLong() to count.toInt()
            },
            config = RulesConfig(fields[6].toInt(), fields[7].toInt()),
            moveCount = fields[11].toInt(),
        )
        SavedGame(
            game = game,
            aiSide = if (fields[8] == NO_AI) null else requireNotNull(sideOf(fields[8].single())),
            difficulty = Difficulty.valueOf(fields[9]),
        )
    }.getOrNull()

    private fun Side?.toChar(): Char = when (this) {
        null -> '.'
        Side.GOAT -> 'G'
        Side.TIGER -> 'T'
    }

    private fun sideOf(char: Char): Side? = when (char) {
        '.' -> null
        'G' -> Side.GOAT
        'T' -> Side.TIGER
        else -> throw IllegalArgumentException("Bad cell '$char'")
    }
}
