package com.matinstudios.baghchal.online

import com.matinstudios.baghchal.engine.GameStatus
import com.matinstudios.baghchal.engine.Move
import com.matinstudios.baghchal.engine.Rules
import com.matinstudios.baghchal.engine.Side

/**
 * The checked moves of a room.
 *
 * @property moves the valid moves, in order, up to the first bad one.
 * @property badMoveBy the side that sent a move that is malformed, illegal, out of turn or sent by
 *   the wrong player; nothing after it is used. Null when every move checks out.
 */
data class CheckedMoves(val moves: List<Move>, val badMoveBy: Side?)

/** Each phone checks every move with the rules engine; there is no referee on the server. */
object OnlineReplay {

    fun check(room: RoomSnapshot): CheckedMoves {
        var game = Rules.initialState()
        val moves = mutableListOf<Move>()
        for (sent in room.moves) {
            val side = game.sideToMove
            val move = MoveCodec.decode(sent.move)
            val valid = move != null &&
                Rules.status(game) == GameStatus.InProgress &&
                sent.by == room.uidOf(side) &&
                Rules.isLegal(game, move)
            if (!valid) return CheckedMoves(moves, badMoveBy = room.sideOf(sent.by) ?: side)
            game = Rules.apply(game, move)
            moves += move
        }
        return CheckedMoves(moves, badMoveBy = null)
    }
}
