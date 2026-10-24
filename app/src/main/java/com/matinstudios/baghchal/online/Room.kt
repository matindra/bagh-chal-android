package com.matinstudios.baghchal.online

import com.matinstudios.baghchal.engine.Side
import kotlin.random.Random

/** A move as stored in a room: the encoded move ([MoveCodec]) and the uid of the player who sent it. */
data class RoomMove(val move: String, val by: String)

/**
 * One online game as stored on the server.
 *
 * @property hostUid the player who created the room; [guestUid] joined it (null while waiting).
 * @property hostSide the side the host plays; the guest plays the other one.
 * @property code the 4-digit code of a private game, null for a random match.
 * @property moves every move so far, in order.
 * @property resignedBy uid of a player who resigned or left, if any.
 * @property online uids currently connected to the room.
 */
data class RoomSnapshot(
    val id: String,
    val hostUid: String,
    val guestUid: String?,
    val hostSide: Side,
    val isPlaying: Boolean,
    val code: String? = null,
    val moves: List<RoomMove> = emptyList(),
    val resignedBy: String? = null,
    val online: Set<String> = emptySet(),
) {
    fun sideOf(uid: String): Side? = when (uid) {
        hostUid -> hostSide
        guestUid -> hostSide.opponent
        else -> null
    }

    fun uidOf(side: Side): String? = if (side == hostSide) hostUid else guestUid

    fun opponentOf(uid: String): String? = when (uid) {
        hostUid -> guestUid
        guestUid -> hostUid
        else -> null
    }
}

/** The 4-digit codes friends share to join a private game. */
object RoomCode {
    const val LENGTH = 4

    fun random(random: Random = Random.Default): String =
        random.nextInt(0, 10_000).toString().padStart(LENGTH, '0')

    /** The code typed by a player, or null if it isn't exactly four digits (spaces are ignored). */
    fun parse(input: String): String? = input.filterNot { it.isWhitespace() }
        .takeIf { it.length == LENGTH && it.all(Char::isDigit) }
}

/** Outcome of trying to join a private game by code. */
sealed interface JoinResult {
    data class Joined(val roomId: String) : JoinResult

    /** No game uses this code, or its host has left. */
    data object NotFound : JoinResult

    /** Someone else already joined. */
    data object Full : JoinResult
}

/** Progress of hosting a private game: first the code to share, then the start once a friend joins. */
sealed interface HostingUpdate {
    data class CodeReady(val code: String) : HostingUpdate
    data class Started(val roomId: String) : HostingUpdate
}

/** Online play could not start: no connection, sign-in failed, or the service refused. */
class OnlineException(message: String, cause: Throwable? = null) : Exception(message, cause)
