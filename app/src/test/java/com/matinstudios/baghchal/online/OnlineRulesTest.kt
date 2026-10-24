package com.matinstudios.baghchal.online

import com.matinstudios.baghchal.engine.Move
import com.matinstudios.baghchal.engine.Side
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OnlineRulesTest {

    // --- MoveCodec ---

    @Test
    fun `every kind of move survives encoding`() {
        for (move in listOf(Move.Place(12), Move.Step(0, 5), Move.Jump(0, 1, 2), Move.Place(0), Move.Step(23, 24))) {
            assertEquals(move, MoveCodec.decode(MoveCodec.encode(move)))
        }
        assertEquals("P12", MoveCodec.encode(Move.Place(12)))
        assertEquals("J0,1,2", MoveCodec.encode(Move.Jump(0, 1, 2)))
    }

    @Test
    fun `malformed moves decode to null`() {
        for (text in listOf("", "P", "X1", "P25", "P-1", "S1", "S1,2,3", "J1,2", "Pa", "S1,", "P1.5")) {
            assertNull(MoveCodec.decode(text), text)
        }
    }

    // --- RoomCode ---

    @Test
    fun `codes are always four digits`() {
        val random = Random(42)
        repeat(1_000) {
            val code = RoomCode.random(random)
            assertEquals(4, code.length)
            assertTrue(code.all(Char::isDigit))
        }
        assertEquals("0007", RoomCode.random(FixedRandom(7)))
    }

    @Test
    fun `typed codes need exactly four digits`() {
        assertEquals("0427", RoomCode.parse("0427"))
        assertEquals("0427", RoomCode.parse(" 04 27 "))
        assertNull(RoomCode.parse("427"))
        assertNull(RoomCode.parse("04271"))
        assertNull(RoomCode.parse("04a7"))
        assertNull(RoomCode.parse(""))
    }

    // --- Room sides ---

    @Test
    fun `the guest plays the side the host didn't pick`() {
        val room = room(hostSide = Side.TIGER)
        assertEquals(Side.TIGER, room.sideOf(HOST))
        assertEquals(Side.GOAT, room.sideOf(GUEST))
        assertNull(room.sideOf("stranger"))
        assertEquals(GUEST, room.uidOf(Side.GOAT))
        assertEquals(HOST, room.opponentOf(GUEST))
    }

    // --- OnlineReplay ---

    @Test
    fun `legal moves from the right players are all accepted`() {
        // Host plays goats, who move first.
        val room = room(Side.GOAT, RoomMove("P1", HOST), RoomMove("J0,1,2", GUEST), RoomMove("P12", HOST))
        val checked = OnlineReplay.check(room)
        assertEquals(listOf(Move.Place(1), Move.Jump(0, 1, 2), Move.Place(12)), checked.moves)
        assertNull(checked.badMoveBy)
    }

    @Test
    fun `an illegal move stops the replay and names its side`() {
        // The tiger steps from 0 to 5, so a goat can't be placed on 5.
        val room = room(Side.GOAT, RoomMove("P12", HOST), RoomMove("S0,5", GUEST), RoomMove("P5", HOST), RoomMove("S5,10", GUEST))
        val checked = OnlineReplay.check(room)
        assertEquals(listOf(Move.Place(12), Move.Step(0, 5)), checked.moves)
        assertEquals(Side.GOAT, checked.badMoveBy)
    }

    @Test
    fun `a move sent by the wrong player is rejected`() {
        // The guest (tigers) tries to place the goats' first move for them.
        val checked = OnlineReplay.check(room(Side.GOAT, RoomMove("P12", GUEST)))
        assertEquals(emptyList(), checked.moves)
        assertEquals(Side.TIGER, checked.badMoveBy)
    }

    @Test
    fun `a garbled move is rejected`() {
        val checked = OnlineReplay.check(room(Side.GOAT, RoomMove("P12", HOST), RoomMove("??", GUEST)))
        assertEquals(1, checked.moves.size)
        assertEquals(Side.TIGER, checked.badMoveBy)
    }

    private class FixedRandom(private val value: Int) : Random() {
        override fun nextBits(bitCount: Int) = value
        override fun nextInt(from: Int, until: Int) = value
    }

    companion object {
        const val HOST = "host-uid"
        const val GUEST = "guest-uid"

        fun room(hostSide: Side, vararg moves: RoomMove) = RoomSnapshot(
            id = "room",
            hostUid = HOST,
            guestUid = GUEST,
            hostSide = hostSide,
            isPlaying = true,
            moves = moves.toList(),
            online = setOf(HOST, GUEST),
        )
    }
}
