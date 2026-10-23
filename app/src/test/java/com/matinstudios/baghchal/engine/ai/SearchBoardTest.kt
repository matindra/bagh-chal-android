package com.matinstudios.baghchal.engine.ai

import com.matinstudios.baghchal.engine.Rules
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchBoardTest {

    @Test
    fun `move generation matches Rules in random positions`() {
        for (state in randomPositions(200)) {
            assertEquals(Rules.legalMoves(state).toSet(), SearchBoard(state).moves().toSet())
        }
    }

    @Test
    fun `make keeps the key equal to the engine position key, unmake restores it`() {
        for (state in randomPositions(100, seed = 7)) {
            val board = SearchBoard(state)
            for (move in Rules.legalMoves(state)) {
                val before = board.key
                val plies = board.pliesSinceCapture
                board.make(move)
                val expected = Rules.apply(state, move)
                assertEquals(expected.positionKey, board.key, "after $move")
                assertEquals(expected.goatsCaptured, board.goatsCaptured)
                assertEquals(expected.pliesSinceCapture, board.pliesSinceCapture)
                board.unmake(move, plies)
                assertEquals(before, board.key, "after undoing $move")
                assertEquals(state.board.map { com.matinstudios.baghchal.engine.PositionKey.code(it) }, board.cells.toList())
            }
        }
    }
}
