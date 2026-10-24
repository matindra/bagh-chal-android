package com.matinstudios.baghchal.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.matinstudios.baghchal.BaghchalApplication
import com.matinstudios.baghchal.ads.Ads
import com.matinstudios.baghchal.ads.RewardedPlacement
import com.matinstudios.baghchal.ads.grantsAssist
import com.matinstudios.baghchal.data.BoardTheme
import com.matinstudios.baghchal.data.SavedGameRepository
import com.matinstudios.baghchal.data.Settings
import com.matinstudios.baghchal.data.SettingsRepository
import com.matinstudios.baghchal.data.SerialWriter
import com.matinstudios.baghchal.data.StatsRepository
import com.matinstudios.baghchal.engine.ai.AiPlayer
import com.matinstudios.baghchal.engine.ai.Difficulty
import com.matinstudios.baghchal.ui.sound.SoundPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameViewModel(
    savedStateHandle: SavedStateHandle,
    private val savedGames: SavedGameRepository,
    private val stats: StatsRepository,
    private val writer: SerialWriter,
    settingsRepository: SettingsRepository,
    boardTheme: Flow<BoardTheme>,
    private val sounds: SoundPlayer,
    private val ads: Ads,
) : ViewModel() {

    private val settings = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    /** Null until settings are loaded, so the board never flashes the default theme. */
    val boardTheme: StateFlow<BoardTheme?> = boardTheme
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val showBanner: StateFlow<Boolean> = ads.bannersEnabled

    /** Undo and Hint each cost a rewarded ad (shown on the buttons); free with Remove Ads or when no ad is loaded. */
    val undoNeedsAd: StateFlow<Boolean> = ads.rewardedReady(RewardedPlacement.UNDO)
    val hintNeedsAd: StateFlow<Boolean> = ads.rewardedReady(RewardedPlacement.HINT)

    private var setup = GameSetup.fromArgs(
        savedStateHandle.get<String>(GameSetup.ARG_AI),
        savedStateHandle.get<String>(GameSetup.ARG_DIFFICULTY),
    )
    private var ai: AiPlayer? = null
    private var aiJob: Job? = null
    private val hintAi = AiPlayer(HINT_DIFFICULTY)
    private var assistJob: Job? = null

    /** Null until the game (new or resumed) is ready. */
    private val _uiState = MutableStateFlow<GameUiState?>(null)
    val uiState: StateFlow<GameUiState?> = _uiState.asStateFlow()

    private val _haptics = Channel<HapticCue>(Channel.BUFFERED)

    /** Haptic cues for moves, already filtered by settings (sounds are played here directly). */
    val haptics: Flow<HapticCue> = _haptics.receiveAsFlow()

    private val _results = Channel<GameResult>(Channel.BUFFERED)

    /** Emitted shortly after a win, so the last move and any lock badges can be seen first. */
    val results: Flow<GameResult> = _results.receiveAsFlow()

    init {
        val resume = savedStateHandle.get<Boolean>(GameSetup.ARG_RESUME) ?: false
        // If the process dies, this screen comes back with the same arguments: continue the saved game then.
        savedStateHandle[GameSetup.ARG_RESUME] = true
        viewModelScope.launch {
            val saved = if (resume) savedGames.load() else null
            if (saved != null) setup = GameSetup(saved.aiSide, saved.difficulty)
            ai = setup.aiSide?.let { AiPlayer(setup.difficulty) }
            update { saved?.let { GameInteraction.fromGame(it.game, it.aiSide) } ?: GameInteraction.newGame(setup.aiSide) }
            startAiTurnIfNeeded()
        }
    }

    fun onPointTapped(point: Int) {
        update { it?.let { ui -> GameInteraction.onPointTapped(ui, point) } }
        startAiTurnIfNeeded()
    }

    fun onNewGame() {
        aiJob?.cancel()
        assistJob?.cancel()
        update { GameInteraction.newGame(setup.aiSide) }
        startAiTurnIfNeeded()
    }

    fun onUndo() {
        if (_uiState.value?.canUndo != true) return
        runAssist(RewardedPlacement.UNDO) {
            aiJob?.cancel()
            update { it?.let(GameInteraction::undo) }
        }
    }

    fun onHint() {
        if (_uiState.value?.canHint != true) return
        runAssist(RewardedPlacement.HINT) {
            val ui = _uiState.value ?: return@runAssist
            if (!ui.canHint) return@runAssist
            update { it?.copy(isHintLoading = true) }
            val move = withContext(Dispatchers.Default) { hintAi.chooseMove(ui.game) }
            update { it?.let { current -> GameInteraction.showHint(current, ui.game, move) } }
        }
    }

    /** Shows the rewarded ad if the buttons said so, then runs [action] unless the player closed it early. */
    private fun runAssist(placement: RewardedPlacement, action: suspend () -> Unit) {
        if (assistJob?.isActive == true) return
        val needsAd = ads.rewardedReady(placement).value
        assistJob = viewModelScope.launch {
            if (needsAd && !ads.showRewarded(placement).grantsAssist()) return@launch
            action()
        }
    }

    /** All state changes go through here so every move is persisted and announced. Main thread only. */
    private fun update(transform: (GameUiState?) -> GameUiState?) {
        val previous = _uiState.value
        val next = transform(previous) ?: return
        _uiState.value = next
        for (action in persistenceActions(previous, next, setup.difficulty)) {
            when (action) {
                is PersistenceAction.Save -> writer.submit { savedGames.save(action.saved) }
                PersistenceAction.ClearSave -> writer.submit { savedGames.clear() }
                is PersistenceAction.RecordResult ->
                    writer.submit { stats.record(action.playerSide, action.difficulty, action.outcome) }
            }
        }
        feedbackFor(previous, next, settings.value)?.let { feedback ->
            feedback.sound?.let(sounds::play)
            feedback.haptic?.let { _haptics.trySend(it) }
        }
        resultFor(previous, next, setup)?.let { result ->
            viewModelScope.launch {
                delay(RESULT_DELAY_MILLIS)
                _results.send(result)
            }
        }
    }

    private fun startAiTurnIfNeeded() {
        val ai = ai ?: return
        val ui = _uiState.value ?: return
        if (!ui.isAiTurn || aiJob?.isActive == true) return
        aiJob = viewModelScope.launch {
            update { it?.copy(isAiThinking = true) }
            val start = System.currentTimeMillis()
            val move = withContext(Dispatchers.Default) { ai.chooseMove(ui.game) }
            // Instant replies are hard to follow; give the player a moment to see their own move land.
            delay(MIN_AI_TURN_MILLIS - (System.currentTimeMillis() - start))
            update { current ->
                current?.let {
                    val next = if (move != null && it.game == ui.game) GameInteraction.applyMove(it, move) else it
                    next.copy(isAiThinking = false)
                }
            }
        }
    }

    companion object {
        private val HINT_DIFFICULTY = Difficulty.MEDIUM
        private const val MIN_AI_TURN_MILLIS = 400L
        private const val RESULT_DELAY_MILLIS = 1_200L

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as BaghchalApplication).container
                GameViewModel(
                    createSavedStateHandle(),
                    container.savedGames,
                    container.stats,
                    container.writer,
                    container.settings,
                    container.boardTheme,
                    container.sounds,
                    container.ads,
                )
            }
        }
    }
}
