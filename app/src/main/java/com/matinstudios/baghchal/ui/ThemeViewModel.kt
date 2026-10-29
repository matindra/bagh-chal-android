package com.matinstudios.baghchal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.matinstudios.baghchal.BaghchalApplication
import com.matinstudios.baghchal.data.PieceSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** The piece set, read at the app root. */
class ThemeViewModel(pieceSet: Flow<PieceSet>) : ViewModel() {

    /** Null until storage is read, so the first frame never shows the wrong pieces. */
    val pieceSet: StateFlow<PieceSet?> = pieceSet
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                ThemeViewModel((this[APPLICATION_KEY] as BaghchalApplication).container.pieceSet)
            }
        }
    }
}
