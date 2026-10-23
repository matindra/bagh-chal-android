package com.matinstudios.baghchal.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.matinstudios.baghchal.engine.GameStatus
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.engine.ai.Difficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class Outcome {
    WIN, LOSS, DRAW;

    companion object {
        /** Result from [playerSide]'s point of view, or null while the game is in progress. */
        fun of(status: GameStatus, playerSide: Side): Outcome? = when (status) {
            GameStatus.InProgress -> null
            is GameStatus.Drawn -> DRAW
            is GameStatus.Won -> if (status.winner == playerSide) WIN else LOSS
        }
    }
}

data class Record(val wins: Int = 0, val losses: Int = 0, val draws: Int = 0) {
    val played: Int get() = wins + losses + draws

    operator fun plus(other: Record) = Record(wins + other.wins, losses + other.losses, draws + other.draws)
}

/** Results of games against the computer, by the side the player played and the difficulty. */
data class Stats(private val records: Map<Pair<Side, Difficulty>, Record> = emptyMap()) {
    fun record(playerSide: Side, difficulty: Difficulty): Record = records[playerSide to difficulty] ?: Record()

    fun total(playerSide: Side): Record =
        Difficulty.entries.fold(Record()) { sum, difficulty -> sum + record(playerSide, difficulty) }
}

class StatsRepository(private val dataStore: DataStore<Preferences>) {

    val stats: Flow<Stats> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            Stats(
                buildMap {
                    for (side in Side.entries) for (difficulty in Difficulty.entries) {
                        fun count(outcome: Outcome) = prefs[key(side, difficulty, outcome)] ?: 0
                        put(side to difficulty, Record(count(Outcome.WIN), count(Outcome.LOSS), count(Outcome.DRAW)))
                    }
                },
            )
        }

    suspend fun record(playerSide: Side, difficulty: Difficulty, outcome: Outcome) {
        dataStore.edit { prefs ->
            val key = key(playerSide, difficulty, outcome)
            prefs[key] = (prefs[key] ?: 0) + 1
        }
    }

    suspend fun reset() {
        dataStore.edit { prefs ->
            for (side in Side.entries) for (difficulty in Difficulty.entries) for (outcome in Outcome.entries) {
                prefs.remove(key(side, difficulty, outcome))
            }
        }
    }

    private fun key(side: Side, difficulty: Difficulty, outcome: Outcome) =
        intPreferencesKey("stats_${side.name}_${difficulty.name}_${outcome.name}".lowercase())
}
