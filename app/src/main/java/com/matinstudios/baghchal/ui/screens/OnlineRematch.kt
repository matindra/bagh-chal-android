package com.matinstudios.baghchal.ui.screens

import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.online.RoomSnapshot

/**
 * Play again after a friend game, as the result dialog shows it.
 *
 * @property nextSide the side this player gets next game (sides swap every game).
 * @property waiting we offered and are waiting for the friend to accept.
 * @property friendAsked the friend offered; tapping Play again starts the game.
 * @property friendLeft the friend has closed the game, so nobody will answer.
 * @property failed the next game couldn't be started (offer withdrawn, or no connection).
 */
data class RematchUi(
    val nextSide: Side,
    val waiting: Boolean = false,
    val friendAsked: Boolean = false,
    val friendLeft: Boolean = false,
    val failed: Boolean = false,
) {
    val canPlayAgain: Boolean get() = !waiting && !friendLeft && !failed
}

/** Rematch state of a finished game for [me], or null when it isn't a friend game (quick match). */
fun rematchUiFor(room: RoomSnapshot, me: String, waiting: Boolean, failed: Boolean): RematchUi? {
    if (!room.isFriendGame) return null
    val mySide = room.sideOf(me) ?: return null
    val friend = room.opponentOf(me) ?: return null
    val friendAsked = room.rematch?.by == friend
    return RematchUi(
        nextSide = mySide.opponent,
        waiting = waiting,
        friendAsked = friendAsked && !waiting,
        friendLeft = friend !in room.online,
        failed = failed,
    )
}
