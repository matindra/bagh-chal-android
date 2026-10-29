package com.matinstudios.baghchal.ui.sound

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MusicTrackTest {

    @Test
    fun `game music plays during a game and home music everywhere else`() {
        assertEquals(MusicTrack.GAME, musicFor(musicEnabled = true, inGame = true))
        assertEquals(MusicTrack.HOME, musicFor(musicEnabled = true, inGame = false))
    }

    @Test
    fun `nothing plays when music is off`() {
        assertNull(musicFor(musicEnabled = false, inGame = true))
        assertNull(musicFor(musicEnabled = false, inGame = false))
    }
}
