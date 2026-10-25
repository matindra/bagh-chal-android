package com.matinstudios.baghchal.ui.screens

import com.matinstudios.baghchal.engine.GameStatus
import com.matinstudios.baghchal.engine.Move
import com.matinstudios.baghchal.engine.Rules
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.online.CheckedMoves
import com.matinstudios.baghchal.online.RoomSnapshot

/**
 * The board of an online game on this phone: the server's moves plus, briefly, this player's own
 * move before the server echoes it back.
 *
 * The opponent's side is stored as [GameUiState.aiSide], so taps are ignored on their turn exactly
 * like on the computer's turn.
 */
data class OnlineSession(val ui: GameUiState, val moves: List<Move>) {

    val mySide: Side get() = ui.aiSide!!.opponent

    /**
     * Catches up with the server's [remote] moves: new ones are played (and animated) one by one;
     * an own move the server hasn't confirmed yet is kept; if the two disagree, the server wins.
     */
    fun syncWith(remote: List<Move>): OnlineSession {
        val agreed = moves.zip(remote).takeWhile { (local, server) -> local == server }.size
        return when {
            agreed == moves.size -> remote.drop(moves.size).fold(this) { session, move -> session.play(move) }
            agreed == remote.size -> this
            else -> start(mySide, remote)
        }
    }

    /** A tap on the board; returns the new session and the move it made, if any, to send. */
    fun tap(point: Int): Pair<OnlineSession, Move?> {
        val next = GameInteraction.onPointTapped(ui, point)
        val moved = next.game.moveCount > ui.game.moveCount
        return if (moved) copy(ui = next, moves = moves + next.lastMove!!) to next.lastMove else copy(ui = next) to null
    }

    private fun play(move: Move) = copy(ui = GameInteraction.applyMove(ui, move), moves = moves + move)

    companion object {
        /** A session for [mySide], with [moves] already on the board (not animated). */
        fun start(mySide: Side, moves: List<Move>): OnlineSession {
            val game = moves.fold(Rules.initialState(), Rules::apply)
            return OnlineSession(GameInteraction.fromGame(game, aiSide = mySide.opponent), moves)
        }
    }
}

/** How an online game ended, from this player's point of view. */
enum class OnlineOutcome { WON, LOST, DRAWN }

enum class OnlineEndReason {
    /** Captures, trapped tigers, blocked goats, or a draw by the rules. */
    RULES,
    RESIGNED,
    OPPONENT_RESIGNED,

    /** The opponent was disconnected for longer than the grace period. */
    OPPONENT_LEFT,

    /** The opponent sent a move the rules don't allow. */
    OPPONENT_INVALID_MOVE,
}

data class OnlineEnd(val outcome: OnlineOutcome, val reason: OnlineEndReason)

/**
 * Whether the game is over for player [me], or null if it goes on. Resigning comes first, then a bad
 * move, then the rules (on this phone's board, which may include a winning move not yet echoed), and
 * finally an opponent who has been gone too long.
 */
fun onlineEndFor(
    room: RoomSnapshot,
    me: String,
    checked: CheckedMoves,
    session: OnlineSession,
    opponentGone: Boolean,
): OnlineEnd? {
    val mySide = session.mySide
    val status = session.ui.status
    return when {
        room.resignedBy == me -> OnlineEnd(OnlineOutcome.LOST, OnlineEndReason.RESIGNED)
        room.resignedBy != null -> OnlineEnd(OnlineOutcome.WON, OnlineEndReason.OPPONENT_RESIGNED)
        checked.badMoveBy == mySide.opponent -> OnlineEnd(OnlineOutcome.WON, OnlineEndReason.OPPONENT_INVALID_MOVE)
        status is GameStatus.Won -> OnlineEnd(
            if (status.winner == mySide) OnlineOutcome.WON else OnlineOutcome.LOST,
            OnlineEndReason.RULES,
        )
        status is GameStatus.Drawn -> OnlineEnd(OnlineOutcome.DRAWN, OnlineEndReason.RULES)
        opponentGone -> OnlineEnd(OnlineOutcome.WON, OnlineEndReason.OPPONENT_LEFT)
        else -> null
    }
}

/** How long a disconnected opponent has to come back before we win. */
const val OPPONENT_GRACE_MILLIS = 45_000L

/**
 * Longer when the opponent hasn't been seen in this game yet: a host who shared the code may still
 * be in the other app (where their phone pauses Bagh Chal) when the friend joins.
 */
const val OPPONENT_ARRIVAL_GRACE_MILLIS = 3 * 60_000L

fun opponentGraceMillis(opponentSeen: Boolean): Long =
    if (opponentSeen) OPPONENT_GRACE_MILLIS else OPPONENT_ARRIVAL_GRACE_MILLIS
