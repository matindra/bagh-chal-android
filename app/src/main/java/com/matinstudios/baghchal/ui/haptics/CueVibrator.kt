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
 *
 * Built-in effects are used only when the phone says it supports them. An unsupported one is
 * swapped by Android for a ~30 ms default pulse, which is too faint to feel on most motors; that
 * is why captures used to feel like nothing at all.
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
                vibrator.vibrate(effectFor(vibrator, cue), VibrationAttributes.createForUsage(VibrationAttributes.USAGE_MEDIA))
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                @Suppress("DEPRECATION")
                vibrator.vibrate(effectFor(vibrator, cue), GAME_AUDIO)
            }
            else -> {
                @Suppress("DEPRECATION")
                vibrator.vibrate(cue.legacyPattern, -1)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun effectFor(vibrator: Vibrator, cue: HapticCue): VibrationEffect = when (cue) {
        HapticCue.GOAT_PLACED ->
            predefined(vibrator, VibrationEffect.EFFECT_TICK) ?: VibrationEffect.createOneShot(20, 120)
        HapticCue.PIECE_TRAPPED ->
            predefined(vibrator, VibrationEffect.EFFECT_CLICK) ?: VibrationEffect.createOneShot(45, 200)
        // A firm double knock, so a capture never feels weaker than a placement.
        HapticCue.GOAT_CAPTURED -> doubleClick(vibrator)
            ?: VibrationEffect.createWaveform(longArrayOf(0, 50, 70, 50), intArrayOf(0, 255, 0, 255), -1)
    }

    /** The phone's own tuned effect, only when it reports real support for it (Android 11+). */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun predefined(vibrator: Vibrator, effect: Int): VibrationEffect? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val support = vibrator.areAllEffectsSupported(effect)
        return if (support == Vibrator.VIBRATION_EFFECT_SUPPORT_YES) VibrationEffect.createPredefined(effect) else null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun doubleClick(vibrator: Vibrator): VibrationEffect? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val click = VibrationEffect.Composition.PRIMITIVE_CLICK
        if (!vibrator.areAllPrimitivesSupported(click)) return null
        return VibrationEffect.startComposition()
            .addPrimitive(click, 1f)
            .addPrimitive(click, 1f, DOUBLE_CLICK_GAP_MILLIS)
            .compose()
    }

    private companion object {
        val GAME_AUDIO: AudioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build()
        const val DOUBLE_CLICK_GAP_MILLIS = 70
    }

    /** On/off timings for phones before Android 8, which have no strength control. */
    private val HapticCue.legacyPattern: LongArray
        get() = when (this) {
            HapticCue.GOAT_PLACED -> longArrayOf(0, 20)
            HapticCue.PIECE_TRAPPED -> longArrayOf(0, 45)
            HapticCue.GOAT_CAPTURED -> longArrayOf(0, 50, 70, 50)
        }
}
