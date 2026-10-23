package com.matinstudios.baghchal.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

/** The single auto-saved in-progress game. */
class SavedGameRepository(private val dataStore: DataStore<Preferences>) {

    val savedGame: Flow<SavedGame?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[KEY]?.let(SavedGameCodec::decode) }

    suspend fun load(): SavedGame? = savedGame.first()

    suspend fun save(saved: SavedGame) {
        dataStore.edit { it[KEY] = SavedGameCodec.encode(saved) }
    }

    suspend fun clear() {
        dataStore.edit { it.remove(KEY) }
    }

    private companion object {
        val KEY = stringPreferencesKey("saved_game")
    }
}
