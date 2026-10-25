package com.matinstudios.baghchal.ui.haptics

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import com.matinstudios.baghchal.ui.screens.HapticCue

/**
 * Plays haptic cues on the phone's vibrator. View haptics (`performHapticFeedback`) were silent on
 * many phones: they follow the system "touch feedback" switch, and some types do nothing at all.
 * Cues are marked as game (media) vibration, not touch feedback, for the same reason; the Haptics
 * switch in Settings is already applied before a cue gets here.
 */
class CueVibrator(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    fun play(cue: HapticCue) {
        val vibrator = vibrator?.takeIf { it.hasVibrator() } ?: return
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                vibrator.vibrate(effectFor(cue), VibrationAttributes.createForUsage(VibrationAttributes.USAGE_MEDIA))
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                @Suppress("DEPRECATION")
                vibrator.vibrate(effectFor(cue), GAME_AUDIO)
            }
            else -> {
                @Suppress("DEPRECATION")
                vibrator.vibrate(cue.fallbackMillis)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun effectFor(cue: HapticCue): VibrationEffect = when (cue) {
        HapticCue.GOAT_PLACED -> predefinedOr(VibrationEffect.EFFECT_TICK, millis = 15, amplitude = 80)
        HapticCue.PIECE_TRAPPED -> predefinedOr(VibrationEffect.EFFECT_CLICK, millis = 30, amplitude = 110)
        HapticCue.GOAT_CAPTURED -> predefinedOr(VibrationEffect.EFFECT_HEAVY_CLICK, millis = 60, amplitude = 255)
    }

    /** The phone's own tuned effect where there is one (Android 10+), else a short one-shot. */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun predefinedOr(effect: Int, millis: Long, amplitude: Int): VibrationEffect =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            VibrationEffect.createPredefined(effect)
        } else {
            VibrationEffect.createOneShot(millis, amplitude)
        }

    private companion object {
        val GAME_AUDIO: AudioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build()
    }

    private val HapticCue.fallbackMillis: Long
        get() = when (this) {
            HapticCue.GOAT_PLACED -> 15
            HapticCue.PIECE_TRAPPED -> 30
            HapticCue.GOAT_CAPTURED -> 60
        }
}
