package com.matinstudios.baghchal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.matinstudios.baghchal.BaghchalApplication
import com.matinstudios.baghchal.data.BoardTheme
import com.matinstudios.baghchal.data.SerialWriter
import com.matinstudios.baghchal.data.SettingsRepository
import com.matinstudios.baghchal.engine.GameStatus
import com.matinstudios.baghchal.engine.Move
import com.matinstudios.baghchal.engine.Rules
import com.matinstudios.baghchal.tutorial.Stage
import com.matinstudios.baghchal.tutorial.Tutorial
import com.matinstudios.baghchal.tutorial.TutorialState
import com.matinstudios.baghchal.ui.sound.SoundEffect
import com.matinstudios.baghchal.ui.sound.SoundPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The guided first game: the player's taps, the tigers' scripted answers and the move sounds. */
class WelcomeViewModel(
    boardTheme: Flow<BoardTheme>,
    soundEnabled: Flow<Boolean>,
    private val settings: SettingsRepository,
    private val writer: SerialWriter,
    private val sounds: SoundPlayer,
) : ViewModel() {

    /** The player's board theme; null until loaded. */
    val boardTheme: StateFlow<BoardTheme?> = boardTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    private val soundOn = soundEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = false)

    private val _state = MutableStateFlow(Tutorial.start())
    val state: StateFlow<TutorialState> = _state.asStateFlow()

    fun onPointTapped(point: Int) {
        val before = _state.value
        val after = Tutorial.tap(before, point)
        _state.value = after
        if (after.moveNumber == before.moveNumber) return
        playSound(after)
        if (after.stage == Stage.TIGER_TURN) {
            viewModelScope.launch {
                // Let the goat land and the player read "The tiger moves…" before it does.
                delay(TIGER_PAUSE_MILLIS)
                val replied = Tutorial.tigerReply(_state.value)
                _state.value = replied
                playSound(replied)
            }
        }
    }

    /** Moves to the next step; false when the tutorial is over. */
    fun onNext(): Boolean {
        val next = Tutorial.next(_state.value) ?: return false
        _state.value = next
        return true
    }

    /** Finished or skipped: later launches open on the home screen. */
    fun onFinished() {
        writer.submit { settings.setTutorialSeen(true) }
    }

    private fun playSound(state: TutorialState) {
        if (!soundOn.value) return
        val won = Rules.status(state.game) is GameStatus.Won
        sounds.play(
            when {
                won -> SoundEffect.PLAYER_WIN
                state.lastMove is Move.Jump -> SoundEffect.TIGER_CAPTURE
                else -> SoundEffect.MOVE
            },
        )
    }

    companion object {
        private const val TIGER_PAUSE_MILLIS = 1_100L

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as BaghchalApplication).container
                WelcomeViewModel(
                    boardTheme = container.boardTheme,
                    soundEnabled = container.settings.settings.map { it.soundEnabled },
                    settings = container.settings,
                    writer = container.writer,
                    sounds = container.sounds,
                )
            }
        }
    }
}
