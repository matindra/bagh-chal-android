package com.matinstudios.baghchal.online

import com.matinstudios.baghchal.engine.GameStatus
import com.matinstudios.baghchal.engine.Rules
import com.matinstudios.baghchal.engine.Side
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BotFallbackOnlineTest {

    private val real = FakeRealOnline()

    private fun TestScope.service(seed: Int = 1) = BotFallbackOnline(
        real = real,
        scope = backgroundScope,
        random = Random(seed),
        aiDispatcher = StandardTestDispatcher(testScheduler),
    )

    @Test
    fun `a real opponent found in time is used`() = runTest {
        val online = service()
        val match = async { online.findRandomMatch() }
        advanceTimeBy(2_000)
        real.match.complete("real-room")
        assertEquals("real-room", match.await())
    }

    @Test
    fun `nobody within five seconds starts a game against the bot`() = runTest {
        val online = service()
        val match = async { online.findRandomMatch() }
        advanceTimeBy(BotFallbackOnline.MAX_WAIT_MILLIS + 1)
        val roomId = match.await()
        assertTrue(real.searchCancelled)

        val room = assertNotNull(online.room(roomId).first())
        assertTrue(room.isPlaying)
        assertEquals(Side.entries.toSet(), setOf(room.sideOf("me"), room.opponentOf("me")?.let(room::sideOf)))
        assertEquals(setOf("me", room.opponentOf("me")), room.online)
    }

    @Test
    fun `the bot never steps in before three seconds`() = runTest {
        val online = service()
        val match = async { online.findRandomMatch() }
        advanceTimeBy(BotFallbackOnline.MIN_WAIT_MILLIS - 1)
        assertTrue(match.isActive)
        match.cancel()
    }

    @Test
    fun `the bot answers every move with a legal one until the game ends`() = runTest {
        for (seed in 1..4) {
            val online = service(seed)
            val roomId = startBotGame(online)
            repeat(12) {
                letBotThink()
                val room = assertNotNull(online.room(roomId).first())
                val checked = OnlineReplay.check(room)
                assertNull(checked.badMoveBy)
                val game = checked.moves.fold(Rules.initialState(), Rules::apply)
                if (Rules.status(game) != GameStatus.InProgress) return@repeat
                assertEquals("me", room.uidOf(game.sideToMove), "after the bot's think time it's my turn")
                val move = Rules.legalMoves(game).first()
                online.sendMove(roomId, room.moves.size, MoveCodec.encode(move))
            }
        }
    }

    @Test
    fun `resigning ends the bot game locally`() = runTest {
        val online = service()
        val roomId = startBotGame(online)
        val room = online.room(roomId) // the game screen holds on to the room like this
        online.resign(roomId)
        letBotThink()
        assertEquals("me", room.first()?.resignedBy)
        assertNull(real.resignedRoom)
    }

    @Test
    fun `real rooms still go to the real service`() = runTest {
        val online = service()
        online.sendMove("real-room", 0, "P12")
        online.resign("real-room")
        assertEquals("real-room", real.sentTo)
        assertEquals("real-room", real.resignedRoom)
    }

    /** Background work (the bot) doesn't count for advanceUntilIdle, so move the clock on by hand. */
    private fun TestScope.letBotThink() {
        advanceTimeBy(BotFallbackOnline.MAX_THINK_MILLIS + 1)
        runCurrent()
    }

    private suspend fun TestScope.startBotGame(online: BotFallbackOnline): String {
        val match = async { online.findRandomMatch() }
        advanceTimeBy(BotFallbackOnline.MAX_WAIT_MILLIS + 1)
        return match.await()
    }

    private class FakeRealOnline : OnlineService {
        override val isAvailable = true
        override val myUid = "me"
        val match = CompletableDeferred<String>()
        var searchCancelled = false
        var sentTo: String? = null
        var resignedRoom: String? = null

        override suspend fun findRandomMatch(): String = try {
            match.await()
        } catch (e: CancellationException) {
            searchCancelled = true
            throw e
        }

        override fun hostPrivateGame(hostSide: Side): Flow<HostingUpdate> = emptyFlow()
        override suspend fun joinPrivateGame(code: String): JoinResult = JoinResult.NotFound
        override fun room(roomId: String): Flow<RoomSnapshot?> = emptyFlow()
        override fun sendMove(roomId: String, index: Int, move: String) {
            sentTo = roomId
        }

        override fun resign(roomId: String) {
            resignedRoom = roomId
        }
    }
}
