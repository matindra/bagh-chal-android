package com.matinstudios.baghchal.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.matinstudios.baghchal.BaghchalApplication
import com.matinstudios.baghchal.data.BoardTheme
import com.matinstudios.baghchal.data.SavedGameRepository
import com.matinstudios.baghchal.data.Settings
import com.matinstudios.baghchal.data.SettingsRepository
import com.matinstudios.baghchal.data.SerialWriter
import com.matinstudios.baghchal.data.StatsRepository
import com.matinstudios.baghchal.engine.ai.AiPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
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
) : ViewModel() {

    private val settings = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    /** Null until settings are loaded, so the board never flashes the default theme. */
    val boardTheme: StateFlow<BoardTheme?> = settingsRepository.settings
        .map { it.boardTheme }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var setup = GameSetup.fromArgs(
        savedStateHandle.get<String>(GameSetup.ARG_AI),
        savedStateHandle.get<String>(GameSetup.ARG_DIFFICULTY),
    )
    private var ai: AiPlayer? = null
    private var aiJob: Job? = null

    /** Null until the game (new or resumed) is ready. */
    private val _uiState = MutableStateFlow<GameUiState?>(null)
    val uiState: StateFlow<GameUiState?> = _uiState.asStateFlow()

    private val _feedback = Channel<Feedback>(Channel.BUFFERED)

    /** Haptic/sound cues for moves, already filtered by settings. */
    val feedback: Flow<Feedback> = _feedback.receiveAsFlow()

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
        update { GameInteraction.newGame(setup.aiSide) }
        startAiTurnIfNeeded()
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
        feedbackFor(previous, next, settings.value)?.let { _feedback.trySend(it) }
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
                )
            }
        }
    }
}
