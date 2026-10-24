package com.matinstudios.baghchal.ui.screens

import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.online.HostingUpdate
import com.matinstudios.baghchal.online.JoinResult
import com.matinstudios.baghchal.online.OnlineException
import com.matinstudios.baghchal.online.OnlineService
import com.matinstudios.baghchal.online.RoomSnapshot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OnlineLobbyViewModelTest {

    private val online = FakeOnline()
    private lateinit var viewModel: OnlineLobbyViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = OnlineLobbyViewModel(online)
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `quick match searches, then opens the game`() = runTest {
        viewModel.onQuickMatch()
        assertEquals(LobbyState.Searching, viewModel.state.value)

        online.match.complete("room-1")
        assertEquals("room-1", viewModel.matched.first())
        assertEquals(LobbyState.Idle, viewModel.state.value)
    }

    @Test
    fun `hosting shows the code, and cancelling closes the game`() = runTest {
        viewModel.onCreateGame(Side.TIGER)
        assertEquals(LobbyState.WaitingForFriend("0427"), viewModel.state.value)
        assertEquals(Side.TIGER, online.hostedSide)

        viewModel.onCancel()
        assertEquals(LobbyState.Idle, viewModel.state.value)
        assertTrue(online.hostingCancelled)
    }

    @Test
    fun `joining with a good code opens the game`() = runTest {
        online.joinResult = JoinResult.Joined("room-2")
        viewModel.onJoin(" 0427 ")
        assertEquals("0427", online.joinedCode)
        assertEquals("room-2", viewModel.matched.first())
    }

    @Test
    fun `joining explains why it didn't work`() = runTest {
        viewModel.onJoin("42")
        assertEquals(LobbyMessage.BAD_CODE, viewModel.messages.first())
        assertEquals(null, online.joinedCode)

        online.joinResult = JoinResult.NotFound
        viewModel.onJoin("1111")
        assertEquals(LobbyMessage.CODE_NOT_FOUND, viewModel.messages.first())

        online.joinResult = JoinResult.Full
        viewModel.onJoin("2222")
        assertEquals(LobbyMessage.GAME_FULL, viewModel.messages.first())
        assertEquals(LobbyState.Idle, viewModel.state.value)
    }

    @Test
    fun `no connection is reported and the lobby is usable again`() = runTest {
        online.match.completeExceptionally(OnlineException("No connection"))
        viewModel.onQuickMatch()
        assertEquals(LobbyMessage.CONNECTION, viewModel.messages.first())
        assertEquals(LobbyState.Idle, viewModel.state.value)
    }

    private class FakeOnline : OnlineService {
        override val isAvailable = true
        override val myUid = "me"
        val match = CompletableDeferred<String>()
        var hostedSide: Side? = null
        var hostingCancelled = false
        var joinResult: JoinResult = JoinResult.NotFound
        var joinedCode: String? = null

        override suspend fun findRandomMatch(): String = match.await()

        override fun hostPrivateGame(hostSide: Side): Flow<HostingUpdate> = flow {
            hostedSide = hostSide
            emit(HostingUpdate.CodeReady("0427"))
            try {
                awaitCancellation()
            } finally {
                hostingCancelled = true
            }
        }

        override suspend fun joinPrivateGame(code: String): JoinResult {
            joinedCode = code
            return joinResult
        }

        override fun room(roomId: String): Flow<RoomSnapshot?> = emptyFlow()
        override fun sendMove(roomId: String, index: Int, move: String) = Unit
        override fun resign(roomId: String) = Unit
    }
}
