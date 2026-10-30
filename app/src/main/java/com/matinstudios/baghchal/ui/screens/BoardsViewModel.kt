package com.matinstudios.baghchal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.matinstudios.baghchal.BaghchalApplication
import com.matinstudios.baghchal.ads.Ads
import com.matinstudios.baghchal.ads.RewardOutcome
import com.matinstudios.baghchal.ads.RewardedPlacement
import com.matinstudios.baghchal.ads.grantsThemeUnlock
import com.matinstudios.baghchal.data.AppIcon
import com.matinstudios.baghchal.data.BoardTheme
import com.matinstudios.baghchal.data.EntitlementRepository
import com.matinstudios.baghchal.data.Entitlements
import com.matinstudios.baghchal.data.LauncherIcons
import com.matinstudios.baghchal.data.PieceSet
import com.matinstudios.baghchal.data.SerialWriter
import com.matinstudios.baghchal.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * @property selected the board in use: a locked choice falls back to the free board.
 * @property selectedPieces the piece set in use: a locked choice falls back to the free set.
 */
data class BoardsUiState(
    val selected: BoardTheme,
    val entitlements: Entitlements,
    val selectedPieces: PieceSet = Entitlements.FREE_PIECE_SET,
)

/** Something a rewarded ad just unlocked, for the "Yay!" note. */
sealed interface JustUnlocked {
    data class Board(val theme: BoardTheme) : JustUnlocked
    data class Pieces(val set: PieceSet) : JustUnlocked
}

class BoardsViewModel(
    private val settings: SettingsRepository,
    private val entitlements: EntitlementRepository,
    private val writer: SerialWriter,
    private val ads: Ads,
    private val launcherIcons: LauncherIcons,
) : ViewModel() {

    /** Null until loaded. */
    val uiState: StateFlow<BoardsUiState?> = combine(settings.settings, entitlements.entitlements) { settings, owned ->
        BoardsUiState(owned.effectiveTheme(settings.boardTheme), owned, owned.effectivePieceSet(settings.pieceSet))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    private val _noAdAvailable = MutableStateFlow(false)
    val noAdAvailable: StateFlow<Boolean> = _noAdAvailable.asStateFlow()

    /** The launcher icon; the system remembers it, so it is read from there rather than stored. */
    private val _appIcon = MutableStateFlow(launcherIcons.current())
    val appIcon: StateFlow<AppIcon> = _appIcon.asStateFlow()

    private val _justUnlocked = MutableStateFlow<JustUnlocked?>(null)

    /** Set after an ad unlocks something; cleared by [onUnlockSeen]. */
    val justUnlocked: StateFlow<JustUnlocked?> = _justUnlocked.asStateFlow()

    private val _justSelected = MutableStateFlow<JustUnlocked?>(null)

    /** Set when an already-unlocked board or piece set is picked, for a short note; cleared by [onSelectedSeen]. */
    val justSelected: StateFlow<JustUnlocked?> = _justSelected.asStateFlow()

    private var unlockJob: Job? = null

    /** A locked board is offered for a rewarded ad; watching it to the end unlocks it for good. */
    fun onBoardSelected(theme: BoardTheme) {
        val state = uiState.value ?: return
        if (state.entitlements.isUnlocked(theme)) {
            writer.submit { settings.setBoardTheme(theme) }
            _justSelected.value = JustUnlocked.Board(theme)
        } else {
            unlockWithAd(JustUnlocked.Board(theme)) {
                entitlements.unlockTheme(theme)
                settings.setBoardTheme(theme)
            }
        }
    }

    /** Piece sets unlock like boards: a rewarded ad watched to the end, or Remove Ads. */
    fun onPieceSetSelected(set: PieceSet) {
        val state = uiState.value ?: return
        if (state.entitlements.isUnlocked(set)) {
            writer.submit { settings.setPieceSet(set) }
            _justSelected.value = JustUnlocked.Pieces(set)
        } else {
            unlockWithAd(JustUnlocked.Pieces(set)) {
                entitlements.unlockPieceSet(set)
                settings.setPieceSet(set)
            }
        }
    }

    /** App icons are free. */
    fun onAppIconSelected(icon: AppIcon) {
        if (icon == _appIcon.value) return
        _appIcon.value = icon
        viewModelScope.launch(Dispatchers.Default) { launcherIcons.select(icon) }
    }

    fun onSelectedSeen() {
        _justSelected.value = null
    }

    fun onUnlockSeen() {
        _justUnlocked.value = null
    }

    private fun unlockWithAd(unlocked: JustUnlocked, unlock: suspend () -> Unit) {
        if (unlockJob?.isActive == true) return
        _noAdAvailable.value = false
        unlockJob = viewModelScope.launch {
            val outcome = ads.showRewarded(RewardedPlacement.THEME)
            when {
                outcome.grantsThemeUnlock() -> {
                    writer.submit { unlock() }
                    _justUnlocked.value = unlocked
                }
                outcome == RewardOutcome.UNAVAILABLE -> _noAdAvailable.value = true
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as BaghchalApplication).container
                BoardsViewModel(
                    container.settings,
                    container.entitlements,
                    container.writer,
                    container.ads,
                    container.launcherIcons,
                )
            }
        }
    }
}
