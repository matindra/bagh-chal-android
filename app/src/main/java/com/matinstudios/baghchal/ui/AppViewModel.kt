package com.matinstudios.baghchal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.matinstudios.baghchal.BaghchalApplication
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

class AppViewModel(tutorialSeen: Flow<Boolean>) : ViewModel() {

    /**
     * Whether the app opens on the welcome tutorial; null until storage is read. Read once per launch,
     * so finishing the tutorial doesn't change the start screen underneath the navigation graph.
     */
    val showWelcome: StateFlow<Boolean?> = flow { emit(!tutorialSeen.first()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                AppViewModel((this[APPLICATION_KEY] as BaghchalApplication).container.settings.tutorialSeen)
            }
        }
    }
}
