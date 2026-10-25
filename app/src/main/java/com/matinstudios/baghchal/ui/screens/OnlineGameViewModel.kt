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
import com.matinstudios.baghchal.data.BoardTheme
import com.matinstudios.baghchal.data.Settings
import com.matinstudios.baghchal.data.SettingsRepository
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.online.MoveCodec
import com.matinstudios.baghchal.online.OnlineReplay
import com.matinstudios.baghchal.online.OnlineService
import com.matinstudios.baghchal.online.RoomSnapshot
import com.matinstudios.baghchal.ui.sound.SoundPlayer
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

/**
 * What the online game screen shows. [session] is null until the room has loaded.
 *
 * @property opponentAway the opponent is disconnected (they lose if they stay away too long).
 * @property end set once the game is over.
 */
data class OnlineGameUi(
    val session: OnlineSession? = null,
    val opponentAway: Boolean = false,
    val end: OnlineEnd? = null,
)

class OnlineGameViewModel(
    savedStateHandle: SavedStateHandle,
    private val online: OnlineService,
    settingsRepository: SettingsRepository,
    boardTheme: Flow<BoardTheme>,
    private val sounds: SoundPlayer,
    private val ads: Ads,
) : ViewModel() {

    private val roomId: String = checkNotNull(savedStateHandle[ARG_ROOM])

    private val settings = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    val boardTheme: StateFlow<BoardTheme?> = boardTheme.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val showBanner: StateFlow<Boolean> = ads.bannersEnabled

    private val _ui = MutableStateFlow(OnlineGameUi())
    val ui: StateFlow<OnlineGameUi> = _ui.asStateFlow()

    private val _haptics = Channel<HapticCue>(Channel.BUFFERED)
    val haptics: Flow<HapticCue> = _haptics.receiveAsFlow()

    private var room: RoomSnapshot? = null
    private var awayTimer: Job? = null
    private var opponentGone = false
    private var opponentSeen = false

    init {
        viewModelScope.launch {
            online.room(roomId).collect { snapshot -> if (snapshot != null) onRoom(snapshot) }
        }
    }

    fun onPointTapped(point: Int) {
        val current = _ui.value
        val session = current.session ?: return
        if (current.end != null) return
        val (next, move) = session.tap(point)
        if (move != null) online.sendMove(roomId, index = session.moves.size, move = MoveCodec.encode(move))
        show(next)
    }

    /** Leaving mid-game concedes it. */
    fun onResign() {
        if (_ui.value.end == null) online.resign(roomId)
    }

    /** Leaving the finished game is the moment between games, where a (capped) interstitial may show. */
    fun onLeaveFinished() = ads.onGameFinished()

    private fun onRoom(snapshot: RoomSnapshot) {
        room = snapshot
        val me = online.myUid ?: return
        val mySide = snapshot.sideOf(me) ?: return
        val checked = OnlineReplay.check(snapshot)
        val session = _ui.value.session?.syncWith(checked.moves) ?: OnlineSession.start(mySide, checked.moves)
        trackOpponent(snapshot, me)
        show(session)
    }

    /** Starts the grace period when the opponent disconnects and cancels it when they're back. */
    private fun trackOpponent(snapshot: RoomSnapshot, me: String) {
        val opponent = snapshot.opponentOf(me)
        val away = opponent != null && opponent !in snapshot.online
        if (opponent != null && !away) opponentSeen = true
        if (away && awayTimer == null) {
            val grace = opponentGraceMillis(opponentSeen)
            awayTimer = viewModelScope.launch {
                delay(grace)
                opponentGone = true
                _ui.value.session?.let(::show)
            }
        } else if (!away) {
            awayTimer?.cancel()
            awayTimer = null
        }
        _ui.value = _ui.value.copy(opponentAway = away)
    }

    /** Every board change goes through here, so moves are heard/felt and the end is noticed. */
    private fun show(session: OnlineSession) {
        val previous = _ui.value
        if (previous.end != null) return
        val room = room ?: return
        val me = online.myUid ?: return
        val end = onlineEndFor(room, me, OnlineReplay.check(room), session, opponentGone)
        previous.session?.ui?.let { before ->
            feedbackFor(before, session.ui, settings.value)?.let { feedback ->
                feedback.sound?.let(sounds::play)
                feedback.haptic?.let { _haptics.trySend(it) }
            }
        }
        _ui.value = previous.copy(session = session, end = end)
        if (end != null) awayTimer?.cancel()
    }

    companion object {
        const val ARG_ROOM = "room"
        const val ROUTE_BASE = "online_game"
        const val ROUTE_PATTERN = "$ROUTE_BASE/{$ARG_ROOM}"
        fun route(roomId: String) = "$ROUTE_BASE/$roomId"

        /** How long a disconnected opponent has to come back before the game is theirs to lose. */

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as BaghchalApplication).container
                OnlineGameViewModel(
                    createSavedStateHandle(),
                    container.online,
                    container.settings,
                    container.boardTheme,
                    container.sounds,
                    container.ads,
                )
            }
        }
    }
}

/** Which side this player has in the game on screen. */
val OnlineGameUi.mySide: Side? get() = session?.mySide
