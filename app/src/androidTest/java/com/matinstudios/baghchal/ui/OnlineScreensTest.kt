package com.matinstudios.baghchal.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.data.BoardTheme
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.ui.screens.OnlineGameContent
import com.matinstudios.baghchal.ui.screens.LobbyState
import com.matinstudios.baghchal.ui.screens.OnlineLobbyContent
import com.matinstudios.baghchal.ui.screens.OnlineLobbyMode
import com.matinstudios.baghchal.ui.screens.OnlineSession
import com.matinstudios.baghchal.ui.theme.BaghchalTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The online lobby and the online board, without a server. */
@RunWith(AndroidJUnit4::class)
class OnlineScreensTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun str(@StringRes id: Int, vararg args: Any) = context.getString(id, *args)
    private fun text(@StringRes id: Int, vararg args: Any) = compose.onNodeWithText(str(id, *args))

    private var lobbyState by mutableStateOf<LobbyState>(LobbyState.Idle)
    private var quickMatches = 0
    private val created = mutableListOf<Side>()
    private val joined = mutableListOf<String>()
    private val shared = mutableListOf<String>()
    private var cancels = 0

    private fun showLobby(mode: OnlineLobbyMode = OnlineLobbyMode.FRIEND, available: Boolean = true) {
        compose.setContent {
            BaghchalTheme {
                OnlineLobbyContent(
                    mode = mode,
                    isAvailable = available,
                    state = lobbyState,
                    onQuickMatch = { quickMatches++ },
                    onCreateGame = { created += it },
                    onJoin = { joined += it },
                    onCancel = { cancels++ },
                    onShareCode = { shared += it },
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun randomLobbyOffersOnlyQuickMatch() {
        showLobby(OnlineLobbyMode.RANDOM)
        text(R.string.online_quick_detail).assertIsDisplayed()
        text(R.string.online_create_button).assertDoesNotExist()
        text(R.string.online_join).assertDoesNotExist()
        text(R.string.online_find).performScrollTo().performClick()
        assertEquals(1, quickMatches)
    }

    @Test
    fun friendLobbyOffersCreateAndJoin() {
        showLobby(OnlineLobbyMode.FRIEND)
        text(R.string.mode_friend).assertIsDisplayed()
        text(R.string.online_find).assertDoesNotExist()

        text(R.string.side_tigers).performScrollTo().performClick()
        text(R.string.online_create_button).performScrollTo().performClick()
        assertEquals(listOf(Side.TIGER), created)

        // Only digits are typed in, at most four.
        compose.onNode(hasSetTextAction()).performScrollTo().performTextInput("04x275")
        text(R.string.online_join).performClick()
        assertEquals(listOf("0427"), joined)
    }

    @Test
    fun hostSeesTheCodeAndCanShareOrCancel() {
        lobbyState = LobbyState.WaitingForFriend("0427", Side.GOAT)
        showLobby()
        compose.onNodeWithContentDescription("0 4 2 7").assertIsDisplayed()
        text(R.string.online_waiting_friend).assertIsDisplayed()
        text(R.string.online_share).performClick()
        assertEquals(listOf("0427"), shared)
        text(R.string.online_cancel_game).performScrollTo().performClick()
        assertEquals(1, cancels)
    }

    @Test
    fun searchingCanBeCancelled() {
        lobbyState = LobbyState.Searching
        showLobby(OnlineLobbyMode.RANDOM)
        text(R.string.online_searching).assertIsDisplayed()
        text(R.string.online_find).assertDoesNotExist()
        text(R.string.cancel).performClick()
        assertEquals(1, cancels)
    }

    @Test
    fun lobbySaysWhenOnlinePlayIsUnavailable() {
        showLobby(OnlineLobbyMode.RANDOM, available = false)
        text(R.string.online_unavailable).assertIsDisplayed()
        text(R.string.online_find).assertDoesNotExist()
    }

    @Test
    fun onlineBoardHasNoUndoOrHintAndShowsWhoseTurn() {
        var session by mutableStateOf(OnlineSession.start(Side.GOAT, emptyList()))
        var opponentAway by mutableStateOf(false)
        compose.setContent {
            BaghchalTheme {
                OnlineGameContent(
                    session = session,
                    opponentAway = opponentAway,
                    end = null,
                    boardTheme = BoardTheme.WOOD,
                    onPointTapped = {},
                    onOpenMenu = {},
                )
            }
        }
        text(R.string.online_turn_place_goat).assertIsDisplayed()
        text(R.string.online_player_you).assertIsDisplayed()
        text(R.string.online_player_opponent).assertIsDisplayed()
        text(R.string.undo).assertDoesNotExist()
        text(R.string.hint).assertDoesNotExist()

        // After a goat is placed it's the tigers' (the opponent's) turn.
        session = session.tap(12).first
        text(R.string.online_their_turn).assertIsDisplayed()
        text(R.string.online_player_thinking, str(R.string.side_tigers)).assertIsDisplayed()

        opponentAway = true
        text(R.string.online_opponent_away).assertIsDisplayed()
    }
}
