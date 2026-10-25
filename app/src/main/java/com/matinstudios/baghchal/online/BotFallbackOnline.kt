package com.matinstudios.baghchal.online

import com.matinstudios.baghchal.engine.GameState
import com.matinstudios.baghchal.engine.GameStatus
import com.matinstudios.baghchal.engine.Rules
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.engine.ai.AiPlayer
import com.matinstudios.baghchal.engine.ai.SearchLimits
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Quick match that never leaves the player waiting long: if [real] finds nobody within a few seconds,
 * the game is played against the computer in a local room. The game screen can't tell the difference:
 * the room has the same shape, the bot moves after a human-looking pause and is always "connected".
 *
 * Private games (codes) always go to [real].
 */
class BotFallbackOnline(
    private val real: OnlineService,
    private val scope: CoroutineScope,
    private val random: Random = Random.Default,
    private val aiDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : OnlineService by real {

    private val botRooms = ConcurrentHashMap<String, MutableStateFlow<RoomSnapshot>>()

    /** Before sign-in (e.g. it timed out) a bot game still needs an id for this player. */
    override val myUid: String? get() = real.myUid ?: LOCAL_UID

    override suspend fun findRandomMatch(): String {
        val wait = random.nextLong(MIN_WAIT_MILLIS, MAX_WAIT_MILLIS + 1)
        return withTimeoutOrNull(wait) { real.findRandomMatch() } ?: startBotRoom()
    }

    override fun room(roomId: String): Flow<RoomSnapshot?> =
        if (isBotRoom(roomId)) botRooms[roomId] ?: flowOf(null) else real.room(roomId)

    override fun sendMove(roomId: String, index: Int, move: String) {
        if (!isBotRoom(roomId)) return real.sendMove(roomId, index, move)
        val room = botRooms[roomId] ?: return
        val me = myUid ?: return
        room.update { if (it.moves.size == index) it.copy(moves = it.moves + RoomMove(move, me)) else it }
    }

    override fun resign(roomId: String) {
        if (!isBotRoom(roomId)) return real.resign(roomId)
        val room = botRooms[roomId] ?: return
        room.update { it.copy(resignedBy = myUid) }
    }

    private fun startBotRoom(): String {
        val me = checkNotNull(myUid)
        val bot = BOT_UID_PREFIX + random.nextLong().toULong().toString(16)
        val id = BOT_ROOM_PREFIX + random.nextLong().toULong().toString(16)
        val room = MutableStateFlow(
            RoomSnapshot(
                id = id,
                hostUid = bot,
                guestUid = me,
                hostSide = Side.entries.random(random),
                isPlaying = true,
                online = setOf(bot, me),
            ),
        )
        botRooms[id] = room
        scope.launch { playBot(room, bot) }
        return id
    }

    /** Answers every move of the player until the game ends or someone resigns. */
    private suspend fun playBot(room: MutableStateFlow<RoomSnapshot>, bot: String) {
        val ai = AiPlayer(BOT_LIMITS, random)
        room.map { it to it.game() }
            .takeWhile { (snapshot, game) -> snapshot.resignedBy == null && game != null }
            .collect { (snapshot, game) ->
                if (game == null || snapshot.uidOf(game.sideToMove) != bot) return@collect
                delay(random.nextLong(MIN_THINK_MILLIS, MAX_THINK_MILLIS + 1))
                val move = withContext(aiDispatcher) { ai.chooseMove(game) } ?: return@collect
                val index = snapshot.moves.size
                room.update {
                    if (it.moves.size == index && it.resignedBy == null) {
                        it.copy(moves = it.moves + RoomMove(MoveCodec.encode(move), bot))
                    } else {
                        it
                    }
                }
            }
        // The screen keeps its own reference to the flow, so the finished room can go.
        botRooms.remove(room.value.id)
    }

    /** The board after the room's moves, or null once the game is over (by the rules or a bad move). */
    private fun RoomSnapshot.game(): GameState? {
        val checked = OnlineReplay.check(this)
        val game = checked.moves.fold(Rules.initialState(), Rules::apply)
        return game.takeIf { checked.badMoveBy == null && Rules.status(it) == GameStatus.InProgress }
    }

    private fun isBotRoom(roomId: String) = roomId.startsWith(BOT_ROOM_PREFIX)

    internal companion object {
        const val MIN_WAIT_MILLIS = 3_000L
        const val MAX_WAIT_MILLIS = 5_000L
        const val MIN_THINK_MILLIS = 700L
        const val MAX_THINK_MILLIS = 2_500L
        const val LOCAL_UID = "local-player"
        const val BOT_ROOM_PREFIX = "bot-"
        private const val BOT_UID_PREFIX = "bot-"

        /** Between Easy and Medium, with some slack, so it plays like a decent human and sometimes slips. */
        val BOT_LIMITS = SearchLimits(maxDepth = 3, timeBudgetMillis = 600, randomMargin = 30)
    }
}
