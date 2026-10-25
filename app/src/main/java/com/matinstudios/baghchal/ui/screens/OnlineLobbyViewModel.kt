package com.matinstudios.baghchal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.matinstudios.baghchal.BaghchalApplication
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.online.HostingUpdate
import com.matinstudios.baghchal.online.JoinResult
import com.matinstudios.baghchal.online.OnlineException
import com.matinstudios.baghchal.online.OnlineService
import com.matinstudios.baghchal.online.RoomCode
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** What the online lobby is doing. */
sealed interface LobbyState {
    data object Idle : LobbyState
    data object Searching : LobbyState
    data object Creating : LobbyState
    data class WaitingForFriend(val code: String, val side: Side) : LobbyState
    data object Joining : LobbyState
}

enum class LobbyMessage { CONNECTION, CODE_NOT_FOUND, GAME_FULL, BAD_CODE }

class OnlineLobbyViewModel(private val online: OnlineService) : ViewModel() {

    val isAvailable: Boolean get() = online.isAvailable

    private val _state = MutableStateFlow<LobbyState>(LobbyState.Idle)
    val state: StateFlow<LobbyState> = _state.asStateFlow()

    private val _messages = Channel<LobbyMessage>(Channel.BUFFERED)
    val messages: Flow<LobbyMessage> = _messages.receiveAsFlow()

    private val _matched = Channel<String>(Channel.BUFFERED)

    /** The room of a game that just started; the screen opens it. */
    val matched: Flow<String> = _matched.receiveAsFlow()

    private var job: Job? = null

    fun onQuickMatch() = start(LobbyState.Searching) {
        _matched.send(online.findRandomMatch())
    }

    fun onCreateGame(side: Side) = start(LobbyState.Creating) {
        online.hostPrivateGame(side).collect { update ->
            when (update) {
                is HostingUpdate.CodeReady -> _state.value = LobbyState.WaitingForFriend(update.code, side)
                is HostingUpdate.Started -> _matched.send(update.roomId)
            }
        }
    }

    fun onJoin(input: String) {
        val code = RoomCode.parse(input) ?: return run { _messages.trySend(LobbyMessage.BAD_CODE) }
        start(LobbyState.Joining) {
            when (val result = online.joinPrivateGame(code)) {
                is JoinResult.Joined -> _matched.send(result.roomId)
                JoinResult.NotFound -> _messages.send(LobbyMessage.CODE_NOT_FOUND)
                JoinResult.Full -> _messages.send(LobbyMessage.GAME_FULL)
            }
        }
    }

    /** Stops searching or closes the hosted game. */
    fun onCancel() {
        job?.cancel()
        _state.value = LobbyState.Idle
    }

    /** Runs one lobby action at a time; the lobby is idle again once it ends, however it ends. */
    private fun start(busy: LobbyState, action: suspend () -> Unit) {
        if (!online.isAvailable || _state.value != LobbyState.Idle) return
        _state.value = busy
        job = viewModelScope.launch {
            try {
                action()
            } catch (_: OnlineException) {
                _messages.send(LobbyMessage.CONNECTION)
            } finally {
                _state.value = LobbyState.Idle
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { OnlineLobbyViewModel((this[APPLICATION_KEY] as BaghchalApplication).container.online) }
        }
    }
}
