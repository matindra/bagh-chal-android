package com.matinstudios.baghchal.data

import com.matinstudios.baghchal.engine.Move
import com.matinstudios.baghchal.engine.Rules
import com.matinstudios.baghchal.engine.RulesConfig
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.engine.ai.Difficulty
import com.matinstudios.baghchal.engine.ai.randomPositions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SavedGameCodecTest {

    @Test
    fun `round-trips random positions with every setup`() {
        val setups = listOf(null to Difficulty.MEDIUM, Side.TIGER to Difficulty.EASY, Side.GOAT to Difficulty.HARD)
        for ((index, game) in randomPositions(60).withIndex()) {
            val (aiSide, difficulty) = setups[index % setups.size]
            val saved = SavedGame(game, aiSide, difficulty)
            assertEquals(saved, SavedGameCodec.decode(SavedGameCodec.encode(saved)))
        }
    }

    @Test
    fun `keeps the move count`() {
        val game = Rules.apply(Rules.apply(Rules.initialState(), Move.Place(12)), Move.Step(0, 5))
        val decoded = SavedGameCodec.decode(SavedGameCodec.encode(SavedGame(game, null, Difficulty.MEDIUM)))
        assertEquals(2, decoded?.game?.moveCount)
    }

    @Test
    fun `keeps repetition history and custom rules`() {
        val game = Rules.initialState(RulesConfig(repetitionsForDraw = 4, maxPliesWithoutCapture = 60))
        val saved = SavedGame(game, Side.TIGER, Difficulty.EASY)
        val decoded = SavedGameCodec.decode(SavedGameCodec.encode(saved))
        assertEquals(game.positionCounts, decoded?.game?.positionCounts)
        assertEquals(game.config, decoded?.game?.config)
    }

    @Test
    fun `rejects corrupt or unknown data`() {
        val valid = SavedGameCodec.encode(SavedGame(Rules.initialState(), null, Difficulty.MEDIUM))
        assertNull(SavedGameCodec.decode(""))
        assertNull(SavedGameCodec.decode("garbage"))
        assertNull(SavedGameCodec.decode(valid.replaceFirst("v2", "v9")), "unknown version")
        assertNull(SavedGameCodec.decode(valid.replaceFirst("v2", "v1").substringBeforeLast("|")), "pre-release v1")
        assertNull(SavedGameCodec.decode(valid.replace("|20|", "|19|")), "goat counts don't add up")
        assertNull(SavedGameCodec.decode(valid.replace("MEDIUM", "IMPOSSIBLE")), "unknown difficulty")
        assertNull(SavedGameCodec.decode(valid.replaceFirst("T", "X")), "bad cell")
    }
}
