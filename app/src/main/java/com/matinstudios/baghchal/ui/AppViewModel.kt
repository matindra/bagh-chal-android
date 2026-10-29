package com.matinstudios.baghchal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.matinstudios.baghchal.BaghchalApplication
import com.matinstudios.baghchal.data.Settings
import com.matinstudios.baghchal.ui.sound.MusicPlayer
import com.matinstudios.baghchal.ui.sound.musicFor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** App-wide state for the navigation graph: the start screen and the background music. */
class AppViewModel(
    tutorialSeen: Flow<Boolean>,
    settings: Flow<Settings>,
    private val music: MusicPlayer,
) : ViewModel() {

    private val inGame = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            combine(settings, inGame) { s, inGame -> musicFor(s.musicEnabled, inGame, s.gameMuted) }
                .distinctUntilChanged()
                .collect(music::play)
        }
    }

    /**
     * Whether the app opens on the welcome tutorial; null until storage is read. Read once per launch,
     * so finishing the tutorial doesn't change the start screen underneath the navigation graph.
     */
    val showWelcome: StateFlow<Boolean?> = flow { emit(!tutorialSeen.first()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

    /** [inGame]: a game is open, possibly under a screen opened from it. */
    fun onScreenChanged(inGame: Boolean) {
        this.inGame.value = inGame
    }

    override fun onCleared() {
        music.play(null)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as BaghchalApplication).container
                AppViewModel(
                    tutorialSeen = container.settings.tutorialSeen,
                    settings = container.settings.settings,
                    music = container.music,
                )
            }
        }
    }
}
