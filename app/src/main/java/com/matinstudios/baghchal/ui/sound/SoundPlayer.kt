package com.matinstudios.baghchal.ui.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.annotation.RawRes
import com.matinstudios.baghchal.R
import java.util.concurrent.ConcurrentHashMap

/** Sound effects in `res/raw` (generated from design/sounds by scripts/convert-sounds.sh). */
enum class SoundEffect(@get:RawRes val resource: Int) {
    MOVE(R.raw.move),
    TIGER_CAPTURE(R.raw.tiger_capture),
    GOAT_CAPTURE(R.raw.goat_capture),
    PLAYER_WIN(R.raw.player_win),
    PLAYER_LOSE(R.raw.player_lose),
}

fun interface SoundPlayer {
    fun play(effect: SoundEffect)
}

/**
 * Plays [SoundEffect]s through a [SoundPool], loading them all up front so the first play is
 * instant. Plays nothing while the app is in the background (see [setInForeground]).
 */
class SoundPoolPlayer(context: Context) : SoundPlayer {
    private val pool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val loaded = ConcurrentHashMap.newKeySet<Int>()
    private val soundIds: Map<SoundEffect, Int>

    @Volatile
    private var inForeground = false

    init {
        pool.setOnLoadCompleteListener { _, soundId, status -> if (status == 0) loaded += soundId }
        soundIds = SoundEffect.entries.associateWith { pool.load(context, it.resource, 1) }
    }

    override fun play(effect: SoundEffect) {
        if (!inForeground) return
        val id = soundIds.getValue(effect)
        if (id in loaded) pool.play(id, 1f, 1f, 1, 0, 1f)
    }

    /** Called by the activity; leaving the foreground also silences anything still playing. */
    fun setInForeground(foreground: Boolean) {
        inForeground = foreground
        if (!foreground) pool.autoPause()
    }

    private companion object {
        const val MAX_STREAMS = 4
    }
}
