package com.matinstudios.baghchal.ui.screens

import com.matinstudios.baghchal.data.Outcome
import com.matinstudios.baghchal.data.SavedGame
import com.matinstudios.baghchal.engine.GameStatus
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.engine.TOTAL_GOATS
import com.matinstudios.baghchal.engine.ai.Difficulty

sealed interface PersistenceAction {
    data class Save(val saved: SavedGame) : PersistenceAction
    data object ClearSave : PersistenceAction
    data class RecordResult(val playerSide: Side, val difficulty: Difficulty, val outcome: Outcome) : PersistenceAction
}

/**
 * What to store after the screen goes from [previous] to [current]:
 * - in-progress games are saved after every move; a game with no moves yet replaces any old save with nothing;
 * - a finished game clears the save and, against the computer, records the result once.
 */
fun persistenceActions(previous: GameUiState?, current: GameUiState, difficulty: Difficulty): List<PersistenceAction> {
    if (previous?.game == current.game) return emptyList()
    val game = current.game
    if (current.status == GameStatus.InProgress) {
        val noMovesYet = game.goatsInHand == TOTAL_GOATS
        return listOf(if (noMovesYet) PersistenceAction.ClearSave else PersistenceAction.Save(SavedGame(game, current.aiSide, difficulty)))
    }
    val aiSide = current.aiSide
    val outcome = aiSide?.let { Outcome.of(current.status, playerSide = it.opponent) }
    val justFinished = previous?.status == GameStatus.InProgress
    return if (outcome != null && justFinished) {
        listOf(PersistenceAction.ClearSave, PersistenceAction.RecordResult(aiSide.opponent, difficulty, outcome))
    } else {
        listOf(PersistenceAction.ClearSave)
    }
}
