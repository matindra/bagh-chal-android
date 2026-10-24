package com.matinstudios.baghchal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.matinstudios.baghchal.BaghchalApplication
import com.matinstudios.baghchal.data.BoardTheme
import com.matinstudios.baghchal.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HowToPlayViewModel(settings: SettingsRepository) : ViewModel() {

    /** The player's board theme for the illustrations; null until loaded. */
    val boardTheme: StateFlow<BoardTheme?> = settings.settings
        .map { it.boardTheme }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                HowToPlayViewModel((this[APPLICATION_KEY] as BaghchalApplication).container.settings)
            }
        }
    }
}
