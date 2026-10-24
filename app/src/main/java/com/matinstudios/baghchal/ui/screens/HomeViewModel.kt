package com.matinstudios.baghchal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.matinstudios.baghchal.BaghchalApplication
import com.matinstudios.baghchal.ads.Ads
import com.matinstudios.baghchal.data.SavedGame
import com.matinstudios.baghchal.data.SavedGameRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(savedGames: SavedGameRepository, ads: Ads) : ViewModel() {

    /** The auto-saved game the player can resume, if any. */
    val savedGame: StateFlow<SavedGame?> = savedGames.savedGame
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    val showBanner: StateFlow<Boolean> = ads.bannersEnabled

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as BaghchalApplication).container
                HomeViewModel(container.savedGames, container.ads)
            }
        }
    }
}
