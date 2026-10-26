package com.matinstudios.baghchal.online

import com.matinstudios.baghchal.engine.Side
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Matchmaking and move exchange for online games. Screens only see this interface; the Firebase
 * version lives in [FirebaseOnlineService], and [NoOnline] stands in when online play isn't set up.
 *
 * Suspending calls throw [OnlineException] when the service can't be reached.
 */
interface OnlineService {
    /** False when the app has no online configuration (e.g. no Firebase project yet, or in tests). */
    val isAvailable: Boolean

    /** This player's id, once signed in. */
    val myUid: String?

    /** Waits for a random opponent and returns the room of the new game. Cancelling stops the search. */
    suspend fun findRandomMatch(): String

    /** Creates a private game where the host plays [hostSide]; completes after the friend joins. Cancelling closes it. */
    fun hostPrivateGame(hostSide: Side): Flow<HostingUpdate>

    suspend fun joinPrivateGame(code: String): JoinResult

    /** Live room state. While collected, this player shows as connected to the room. */
    fun room(roomId: String): Flow<RoomSnapshot?>

    /** Sends this player's move number [index] (0-based). Queued and retried while offline. */
    fun sendMove(roomId: String, index: Int, move: String)

    /** Concedes the game; the opponent wins. */
    fun resign(roomId: String)

    /**
     * After a finished friend game: offers to play again (sides swap), or accepts the friend's offer.
     * Throws [OnlineException] when the server can't be reached.
     */
    suspend fun playAgain(roomId: String): RematchResult

    /** Takes back this player's unanswered offer [nextRoomId] (fire and forget). */
    fun withdrawRematch(roomId: String, nextRoomId: String)
}

/** Online play switched off: no configuration, or instrumented tests. */
object NoOnline : OnlineService {
    override val isAvailable = false
    override val myUid: String? = null
    override suspend fun findRandomMatch(): String = throw unavailable()
    override fun hostPrivateGame(hostSide: Side): Flow<HostingUpdate> = throw unavailable()
    override suspend fun joinPrivateGame(code: String): JoinResult = throw unavailable()
    override fun room(roomId: String): Flow<RoomSnapshot?> = emptyFlow()
    override fun sendMove(roomId: String, index: Int, move: String) = Unit
    override fun resign(roomId: String) = Unit
    override suspend fun playAgain(roomId: String): RematchResult = throw unavailable()
    override fun withdrawRematch(roomId: String, nextRoomId: String) = Unit

    private fun unavailable() = OnlineException("Online play is not configured")
}
