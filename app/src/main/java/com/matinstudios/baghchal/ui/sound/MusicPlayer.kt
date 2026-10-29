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
import android.os.Handler
import android.os.Looper
import androidx.annotation.RawRes
import com.matinstudios.baghchal.R

/** Background music in `res/raw` (generated from design/sounds by scripts/convert-sounds.sh). */
enum class MusicTrack(
    @get:RawRes val resource: Int,
    /** Silence before the track starts, so the screen settles first. */
    val startDelayMs: Long,
    /** How long the track takes to rise from silence to [volume]. */
    val fadeInMs: Long,
    /** Loudest it plays, 0..1. */
    val volume: Float,
) {
    /** Every screen except a game in progress. */
    HOME(R.raw.music_home, startDelayMs = 0, fadeInMs = 1_200, volume = 1f),

    /**
     * While a game is in progress, including screens opened from it (Settings, How to Play, Stats).
     * Starts after 5 s, rises slowly and stays low so it never gets in the way of thinking.
     */
    GAME(R.raw.music_game, startDelayMs = 5_000, fadeInMs = 8_000, volume = 0.4f),
}

/** What should be playing: nothing when music is off, or during a game muted with its mute button. */
fun musicFor(musicEnabled: Boolean, inGame: Boolean, gameMuted: Boolean = false): MusicTrack? = when {
    !musicEnabled -> null
    inGame -> if (gameMuted) null else MusicTrack.GAME
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
    private val handler = Handler(Looper.getMainLooper())
    private var pendingStart: Runnable? = null // waiting out the current track's start delay

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
                cancelPendingStart()
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
            // Still waiting out the start delay, or the wait was interrupted: (re)start it.
            player == null -> start(track)
            player.isPlaying != true -> resume()
            // Wanted again while fading out (or still fading in): fade back up from where it is.
            volume < track.volume -> fadeTo(track.volume, track.fadeInMs)
        }
    }

    /** Starts [track] after its start delay (unless something else is wanted by then). */
    private fun start(track: MusicTrack) {
        current = track
        if (pendingStart != null) return
        val begin = Runnable {
            pendingStart = null
            if (wanted == track && current == track && player == null) create(track)
        }
        pendingStart = begin
        handler.postDelayed(begin, track.startDelayMs)
    }

    private fun create(track: MusicTrack) {
        if (!inForeground || stoppedForFocus || pausedForFocus || !takeFocus()) return
        val created = MediaPlayer.create(context, track.resource, attributes, audioManager.generateAudioSessionId())
            ?: return
        created.isLooping = true
        player = created
        resume()
    }

    private fun resume() {
        val player = player ?: return
        val track = current ?: return
        if (!takeFocus()) return
        setVolume(0f)
        player.start()
        fadeTo(track.volume, track.fadeInMs)
    }

    private fun cancelPendingStart() {
        pendingStart?.let(handler::removeCallbacks)
        pendingStart = null
    }

    private fun release() {
        cancelPendingStart()
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
        const val FADE_OUT_MS = 500L
    }
}
