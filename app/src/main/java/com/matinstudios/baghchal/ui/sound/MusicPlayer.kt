package com.matinstudios.baghchal.ui.sound

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import androidx.annotation.RawRes
import com.matinstudios.baghchal.R

/** Background music in `res/raw` (generated from design/sounds by scripts/convert-sounds.sh). */
enum class MusicTrack(@get:RawRes val resource: Int) {
    /** Every screen except a game in progress. */
    HOME(R.raw.music_home),

    /** While a game is in progress, including screens opened from it (Settings, How to Play, Stats). */
    GAME(R.raw.music_game),
}

/** What should be playing: nothing when music is off. */
fun musicFor(musicEnabled: Boolean, inGame: Boolean): MusicTrack? = when {
    !musicEnabled -> null
    inGame -> MusicTrack.GAME
    else -> MusicTrack.HOME
}

/** Main thread only. */
interface MusicPlayer {
    /** Loops [track], cross-fading from the current one; null fades out and stops. */
    fun play(track: MusicTrack?)

    /** Called by the activity: music pauses while the app is in the background. */
    fun setInForeground(foreground: Boolean)
}

object NoMusic : MusicPlayer {
    override fun play(track: MusicTrack?) = Unit
    override fun setInForeground(foreground: Boolean) = Unit
}

/**
 * Loops a [MusicTrack] with [MediaPlayer], fading between tracks.
 *
 * Plays well with other audio: it stays quiet if another app is already playing music when it would
 * start, holds audio focus while playing, pauses for calls and notifications that need focus, and
 * stops when another app takes over (until the app next comes to the foreground).
 */
class MediaMusicPlayer(context: Context) : MusicPlayer {
    private val context = context.applicationContext
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasFocus = true
                pausedForFocus = false
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> pausedForFocus = true
            AudioManager.AUDIOFOCUS_LOSS -> {
                abandonFocus()
                stoppedForFocus = true
            }
        }
        update()
    }
    private val focusRequest: AudioFocusRequest? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener(focusListener)
            .build()
    } else {
        null
    }

    private var wanted: MusicTrack? = null
    private var inForeground = false
    private var hasFocus = false
    private var pausedForFocus = false // a call or notification; resumes when it ends
    private var stoppedForFocus = false // another app took over; cleared when the app comes back

    private var current: MusicTrack? = null
    private var player: MediaPlayer? = null
    private var volume = 0f
    private var fade: ValueAnimator? = null

    override fun play(track: MusicTrack?) {
        wanted = track
        update()
    }

    override fun setInForeground(foreground: Boolean) {
        inForeground = foreground
        if (foreground) stoppedForFocus = false
        update()
    }

    /** Brings playback in line with what is wanted; safe to call any number of times. */
    private fun update() {
        val track = wanted
        val player = player
        when {
            track == null -> fadeTo(0f, FADE_OUT_MS) {
                release()
                abandonFocus()
            }
            !inForeground || stoppedForFocus || pausedForFocus -> {
                fade?.cancel()
                player?.pause()
                if (!inForeground) abandonFocus()
            }
            track != current -> if (player?.isPlaying == true) {
                fadeTo(0f, FADE_OUT_MS) {
                    release()
                    update()
                }
            } else {
                release()
                start(track)
            }
            player?.isPlaying != true -> resume()
            // Wanted again while fading out (or still fading in): fade back up from where it is.
            volume < 1f -> fadeTo(1f, FADE_IN_MS)
        }
    }

    private fun start(track: MusicTrack) {
        if (!takeFocus()) return
        val created = MediaPlayer.create(context, track.resource, attributes, audioManager.generateAudioSessionId())
            ?: return
        created.isLooping = true
        player = created
        current = track
        resume()
    }

    private fun resume() {
        val player = player ?: return
        if (!takeFocus()) return
        setVolume(0f)
        player.start()
        fadeTo(1f, FADE_IN_MS)
    }

    private fun release() {
        fade?.cancel()
        player?.release()
        player = null
        current = null
        volume = 0f
    }

    private fun fadeTo(target: Float, durationMs: Long, onEnd: () -> Unit = {}) {
        fade?.cancel()
        if (player?.isPlaying != true) {
            setVolume(target)
            onEnd()
            return
        }
        fade = ValueAnimator.ofFloat(volume, target).apply {
            duration = durationMs
            addUpdateListener { setVolume(it.animatedValue as Float) }
            addListener(object : AnimatorListenerAdapter() {
                private var cancelled = false
                override fun onAnimationCancel(animation: Animator) {
                    cancelled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (!cancelled) onEnd()
                }
            })
            start()
        }
    }

    private fun setVolume(value: Float) {
        volume = value
        player?.setVolume(value, value)
    }

    /** Doesn't interrupt music the person is already listening to in another app. */
    private fun takeFocus(): Boolean {
        if (hasFocus) return true
        if (audioManager.isMusicActive) return false
        val result = if (focusRequest != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        hasFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasFocus
    }

    private fun abandonFocus() {
        if (!hasFocus) return
        hasFocus = false
        pausedForFocus = false
        if (focusRequest != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusListener)
        }
    }

    private companion object {
        const val FADE_IN_MS = 1_200L
        const val FADE_OUT_MS = 500L
    }
}
