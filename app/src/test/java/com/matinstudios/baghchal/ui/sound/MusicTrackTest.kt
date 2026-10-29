package com.matinstudios.baghchal.ui.sound

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    @Test
    fun `the mute button silences games only`() {
        assertNull(musicFor(musicEnabled = true, inGame = true, gameMuted = true))
        assertEquals(MusicTrack.HOME, musicFor(musicEnabled = true, inGame = false, gameMuted = true))
    }

    @Test
    fun `game music waits 5 seconds, rises slowly and stays quieter than home music`() {
        assertEquals(5_000L, MusicTrack.GAME.startDelayMs)
        assertTrue(MusicTrack.GAME.fadeInMs > MusicTrack.HOME.fadeInMs)
        assertTrue(MusicTrack.GAME.volume < MusicTrack.HOME.volume)
        assertEquals(0L, MusicTrack.HOME.startDelayMs)
    }
}
