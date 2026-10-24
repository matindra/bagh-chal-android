package com.matinstudios.baghchal.data

import android.content.Context
import com.matinstudios.baghchal.ui.sound.SoundPoolPlayer
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** App-wide singletons, created once in the Application. */
class AppContainer(context: Context) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val dataStore = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("baghchal") },
    )

    val writer = SerialWriter(appScope)
    val savedGames = SavedGameRepository(dataStore)
    val stats = StatsRepository(dataStore)
    val settings = SettingsRepository(dataStore)

    /** Created at app start so every sound is loaded before the first game. */
    val sounds = SoundPoolPlayer(context)
}
