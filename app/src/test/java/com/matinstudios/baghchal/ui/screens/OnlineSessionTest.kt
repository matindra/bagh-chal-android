package com.matinstudios.baghchal.ui.screens

import com.matinstudios.baghchal.engine.Board
import com.matinstudios.baghchal.engine.GameStatus
import com.matinstudios.baghchal.engine.Move
import com.matinstudios.baghchal.engine.Rules
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.engine.WinReason
import com.matinstudios.baghchal.online.CheckedMoves
import com.matinstudios.baghchal.online.RoomSnapshot
import com.matinstudios.baghchal.online.OnlineRulesTest.Companion.GUEST
import com.matinstudios.baghchal.online.OnlineRulesTest.Companion.HOST
import com.matinstudios.baghchal.online.OnlineRulesTest.Companion.room
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class OnlineSessionTest {

    private val goats = OnlineSession.start(Side.GOAT, emptyList())

    // --- Taps and sync ---

    @Test
    fun `tapping an empty point on my turn makes a move to send`() {
        val (next, move) = goats.tap(12)
        assertEquals(Move.Place(12), move)
        assertEquals(listOf<Move>(Move.Place(12)), next.moves)
        assertEquals(Side.TIGER, next.ui.game.sideToMove)
    }

    @Test
    fun `taps on the opponent's turn do nothing`() {
        val tigers = OnlineSession.start(Side.TIGER, emptyList())
        val (next, move) = tigers.tap(12)
        assertNull(move)
        assertEquals(tigers.ui.game, next.ui.game)
    }

    @Test
    fun `the opponent's new moves are played and animated`() {
        val (placed, _) = goats.tap(12)
        val synced = placed.syncWith(listOf(Move.Place(12), Move.Step(0, 5)))
        assertEquals(listOf(Move.Place(12), Move.Step(0, 5)), synced.moves)
        assertEquals(Move.Step(0, 5), synced.ui.lastMove)
        assertEquals(Side.GOAT, synced.ui.game.sideToMove)
    }

    @Test
    fun `my move waiting for the server is kept`() {
        val (placed, _) = goats.tap(12)
        assertSame(placed, placed.syncWith(emptyList()))
        assertSame(placed, placed.syncWith(listOf(Move.Place(12))))
    }

    @Test
    fun `when this phone and the server disagree the server wins`() {
        val (placed, _) = goats.tap(12)
        val synced = placed.syncWith(listOf(Move.Place(7), Move.Step(0, 5)))
        assertEquals(listOf(Move.Place(7), Move.Step(0, 5)), synced.moves)
        assertEquals(Side.GOAT, synced.ui.game.board[7])
        assertNull(synced.ui.game.board[12])
    }

    @Test
    fun `joining a game in progress starts from its moves`() {
        val session = OnlineSession.start(Side.TIGER, listOf(Move.Place(12)))
        assertEquals(Side.TIGER, session.mySide)
        assertEquals(Side.TIGER, session.ui.game.sideToMove)
        assertTrue(session.ui.targets.isEmpty()) // nothing selected yet
    }

    // --- End of game ---

    @Test
    fun `resigning loses and the opponent resigning wins`() {
        val resigned = room(Side.GOAT).copy(resignedBy = HOST)
        assertEquals(OnlineEnd(OnlineOutcome.LOST, OnlineEndReason.RESIGNED), end(resigned, HOST, goats))
        val tigers = OnlineSession.start(Side.TIGER, emptyList())
        assertEquals(OnlineEnd(OnlineOutcome.WON, OnlineEndReason.OPPONENT_RESIGNED), end(resigned, GUEST, tigers))
    }

    @Test
    fun `a bad move from the opponent wins the game`() {
        val checked = CheckedMoves(emptyList(), badMoveBy = Side.TIGER)
        val result = onlineEndFor(room(Side.GOAT), HOST, checked, goats, opponentGone = false)
        assertEquals(OnlineEnd(OnlineOutcome.WON, OnlineEndReason.OPPONENT_INVALID_MOVE), result)
    }

    @Test
    fun `a rules win or loss is reported for my side`() {
        val tigersWon = sessionWithStatus(Side.GOAT, GameStatus.Won(Side.TIGER, WinReason.CAPTURES))
        assertEquals(OnlineEnd(OnlineOutcome.LOST, OnlineEndReason.RULES), end(room(Side.GOAT), HOST, tigersWon))
        val asTigers = sessionWithStatus(Side.TIGER, GameStatus.Won(Side.TIGER, WinReason.CAPTURES))
        assertEquals(OnlineEnd(OnlineOutcome.WON, OnlineEndReason.RULES), end(room(Side.GOAT), GUEST, asTigers))
    }

    @Test
    fun `an opponent gone too long loses, but only while the game is on`() {
        assertNull(end(room(Side.GOAT), HOST, goats))
        assertEquals(
            OnlineEnd(OnlineOutcome.WON, OnlineEndReason.OPPONENT_LEFT),
            onlineEndFor(room(Side.GOAT), HOST, CheckedMoves(emptyList(), null), goats, opponentGone = true),
        )
    }

    @Test
    fun `a real game played through sessions ends the same on both phones`() {
        // Tigers capture five goats placed in front of them; both phones see the same result.
        var host = OnlineSession.start(Side.GOAT, emptyList())
        var guest = OnlineSession.start(Side.TIGER, emptyList())
        var server = emptyList<Move>()
        while (host.ui.status == GameStatus.InProgress) {
            val toMove = if (host.ui.game.sideToMove == Side.GOAT) host else guest
            val move = Rules.legalMoves(toMove.ui.game).let { moves -> moves.firstOrNull { it is Move.Jump } ?: moves.first() }
            server = server + move
            host = host.syncWith(server)
            guest = guest.syncWith(server)
        }
        assertEquals(host.ui.game, guest.ui.game)
        val hostEnd = end(room(Side.GOAT), HOST, host)
        val guestEnd = end(room(Side.GOAT), GUEST, guest)
        assertTrue(hostEnd != null && guestEnd != null)
        if (hostEnd.outcome == OnlineOutcome.DRAWN) {
            assertEquals(OnlineOutcome.DRAWN, guestEnd.outcome)
        } else {
            assertTrue(hostEnd.outcome != guestEnd.outcome)
        }
        assertTrue(server.size < Board.POINTS * 20)
    }

    private fun end(room: RoomSnapshot, me: String, session: OnlineSession) =
        onlineEndFor(room, me, CheckedMoves(session.moves, null), session, opponentGone = false)

    private fun sessionWithStatus(mySide: Side, status: GameStatus): OnlineSession {
        val session = OnlineSession.start(mySide, emptyList())
        return session.copy(ui = session.ui.copy(status = status))
    }

    @Test
    fun `an opponent not seen yet gets longer to arrive than one who dropped out`() {
        assertEquals(OPPONENT_GRACE_MILLIS, opponentGraceMillis(opponentSeen = true))
        assertEquals(OPPONENT_ARRIVAL_GRACE_MILLIS, opponentGraceMillis(opponentSeen = false))
        assertTrue(OPPONENT_ARRIVAL_GRACE_MILLIS > OPPONENT_GRACE_MILLIS)
    }
}
