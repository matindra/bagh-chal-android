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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class WelcomeViewModel(
    boardTheme: Flow<BoardTheme>,
    private val settings: SettingsRepository,
    private val writer: SerialWriter,
) : ViewModel() {

    /** The player's board theme for the illustrations; null until loaded. */
    val boardTheme: StateFlow<BoardTheme?> = boardTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    /** Finished or skipped: later launches open on the home screen. */
    fun onFinished() {
        writer.submit { settings.setTutorialSeen(true) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as BaghchalApplication).container
                WelcomeViewModel(container.boardTheme, container.settings, container.writer)
            }
        }
    }
}
