package com.matinstudios.baghchal.ui.screens

import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.online.RematchOffer
import com.matinstudios.baghchal.online.RoomSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OnlineRematchTest {

    private fun room(
        code: String? = "0427",
        online: Set<String> = setOf(ME, FRIEND),
        rematch: RematchOffer? = null,
    ) = RoomSnapshot(
        id = "r1",
        hostUid = ME,
        guestUid = FRIEND,
        hostSide = Side.GOAT,
        isPlaying = true,
        code = code,
        online = online,
        rematch = rematch,
    )

    private fun ui(room: RoomSnapshot, me: String = ME, waiting: Boolean = false, failed: Boolean = false) =
        rematchUiFor(room, me, waiting, failed)

    @Test
    fun `quick matches have no rematch, they go back to the lobby`() {
        assertNull(ui(room(code = null)))
    }

    @Test
    fun `sides swap for the next game`() {
        assertEquals(Side.TIGER, ui(room())!!.nextSide)
        assertEquals(Side.GOAT, ui(room(), me = FRIEND)!!.nextSide)
    }

    @Test
    fun `after a friend game either player can offer to play again`() {
        val rematch = ui(room())!!
        assertTrue(rematch.canPlayAgain)
        assertFalse(rematch.friendAsked)
        assertFalse(rematch.waiting)
    }

    @Test
    fun `the friend sees the offer, the one who offered waits`() {
        val offered = room(rematch = RematchOffer("r2", by = ME))
        val mine = ui(offered, waiting = true)!!
        assertTrue(mine.waiting)
        assertFalse(mine.friendAsked)
        assertFalse(mine.canPlayAgain)

        val theirs = ui(offered, me = FRIEND)!!
        assertTrue(theirs.friendAsked)
        assertTrue(theirs.canPlayAgain)
    }

    @Test
    fun `no play again once the friend has left`() {
        val rematch = ui(room(online = setOf(ME)))!!
        assertTrue(rematch.friendLeft)
        assertFalse(rematch.canPlayAgain)
    }

    @Test
    fun `a failed rematch can't be retried`() {
        assertFalse(ui(room(), failed = true)!!.canPlayAgain)
    }

    @Test
    fun `a stranger to the room gets nothing`() {
        assertNull(ui(room(), me = "someone-else"))
    }

    private companion object {
        const val ME = "me"
        const val FRIEND = "friend"
    }
}
