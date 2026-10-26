package com.matinstudios.baghchal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.matinstudios.baghchal.BaghchalApplication
import com.matinstudios.baghchal.data.BoardTheme
import com.matinstudios.baghchal.data.PieceSet
import com.matinstudios.baghchal.data.SerialWriter
import com.matinstudios.baghchal.data.SettingsRepository
import com.matinstudios.baghchal.data.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** The saved light/dark choice (changed from the side menu) and the piece set, read at the app root. */
class ThemeViewModel(
    private val repository: SettingsRepository,
    private val writer: SerialWriter,
    pieceSet: Flow<PieceSet>,
    boardTheme: Flow<BoardTheme>,
) : ViewModel() {

    /** The board in use; the app's neutral colours follow it. Null until storage is read. */
    val boardTheme: StateFlow<BoardTheme?> = boardTheme
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

    /** Null until storage is read, so the first frame never shows the wrong pieces. */
    val pieceSet: StateFlow<PieceSet?> = pieceSet
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

    /** Null until storage is read; callers follow the system meanwhile. */
    val themeMode: StateFlow<ThemeMode?> = repository.settings
        .map { it.themeMode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

    fun onThemeModeChanged(mode: ThemeMode) {
        writer.submit { repository.setThemeMode(mode) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as BaghchalApplication).container
                ThemeViewModel(container.settings, container.writer, container.pieceSet, container.boardTheme)
            }
        }
    }
}
