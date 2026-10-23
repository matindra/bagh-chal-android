package com.matinstudios.baghchal.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matinstudios.baghchal.engine.ai.AiPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    val setup = GameSetup.fromArgs(
        savedStateHandle.get<String>(GameSetup.ARG_AI),
        savedStateHandle.get<String>(GameSetup.ARG_DIFFICULTY),
    )
    private val ai = setup.aiSide?.let { AiPlayer(setup.difficulty) }
    private var aiJob: Job? = null

    private val _uiState = MutableStateFlow(GameInteraction.newGame(setup.aiSide))
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        startAiTurnIfNeeded()
    }

    fun onPointTapped(point: Int) {
        _uiState.update { GameInteraction.onPointTapped(it, point) }
        startAiTurnIfNeeded()
    }

    fun onNewGame() {
        aiJob?.cancel()
        _uiState.value = GameInteraction.newGame(setup.aiSide)
        startAiTurnIfNeeded()
    }

    private fun startAiTurnIfNeeded() {
        val ai = ai ?: return
        val ui = _uiState.value
        if (!ui.isAiTurn || aiJob?.isActive == true) return
        aiJob = viewModelScope.launch {
            _uiState.update { it.copy(isAiThinking = true) }
            val start = System.currentTimeMillis()
            val move = withContext(Dispatchers.Default) { ai.chooseMove(ui.game) }
            // Instant replies are hard to follow; give the player a moment to see their own move land.
            delay(MIN_AI_TURN_MILLIS - (System.currentTimeMillis() - start))
            _uiState.update { current ->
                val next = if (move != null && current.game == ui.game) GameInteraction.applyMove(current, move) else current
                next.copy(isAiThinking = false)
            }
        }
    }

    private companion object {
        const val MIN_AI_TURN_MILLIS = 400L
    }
}
