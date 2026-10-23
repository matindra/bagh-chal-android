package com.matinstudios.baghchal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.matinstudios.baghchal.BaghchalApplication
import com.matinstudios.baghchal.data.BoardTheme
import com.matinstudios.baghchal.data.SerialWriter
import com.matinstudios.baghchal.data.Settings
import com.matinstudios.baghchal.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel(private val repository: SettingsRepository, private val writer: SerialWriter) : ViewModel() {

    /** Null until loaded. */
    val settings: StateFlow<Settings?> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    fun onSoundChanged(enabled: Boolean) {
        writer.submit { repository.setSoundEnabled(enabled) }
    }

    fun onHapticsChanged(enabled: Boolean) {
        writer.submit { repository.setHapticsEnabled(enabled) }
    }

    fun onBoardThemeSelected(theme: BoardTheme) {
        writer.submit { repository.setBoardTheme(theme) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as BaghchalApplication).container
                SettingsViewModel(container.settings, container.writer)
            }
        }
    }
}
