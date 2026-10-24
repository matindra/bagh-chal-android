package com.matinstudios.baghchal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.matinstudios.baghchal.BaghchalApplication
import com.matinstudios.baghchal.data.BoardTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HowToPlayViewModel(boardTheme: Flow<BoardTheme>) : ViewModel() {

    /** The player's board theme for the illustrations; null until loaded. */
    val boardTheme: StateFlow<BoardTheme?> = boardTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                HowToPlayViewModel((this[APPLICATION_KEY] as BaghchalApplication).container.boardTheme)
            }
        }
    }
}
