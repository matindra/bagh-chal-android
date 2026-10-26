package com.matinstudios.baghchal.ui.board

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class Actor { TIGER, GOAT }

/**
 * One character in the home-screen scene. [x] and [lift] are in scene units (the line's points
 * sit at x = 1, 3, 5, 7, 9); scales and [rotation] (degrees) are applied around the piece's foot.
 */
data class Sprite(
    val actor: Actor,
    val x: Float,
    val lift: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val rotation: Float = 0f,
    val alpha: Float = 1f,
)

/** One moment of the scene: the characters, a capture burst, and the trap's lock. */
data class ChaseFrame(
    val sprites: List<Sprite>,
    /** Burst at the captured goat's x, 0..1 through its life; null when there is none. */
    val burstX: Float?,
    val burstProgress: Float,
    /** 0 = no lock yet, 1 = fully shown; the ring pulses with [trapMillis]. */
    val lockScale: Float,
    val trapMillis: Float,
    /** Overall visibility, for fading between loops. */
    val alpha: Float,
)

/**
 * A wordless little story of the game, played on a loop above the home menu: goats hop onto
 * the line, the tiger leaps over a careless one (it vanishes in a puff), then more goats box the
 * tiger in until it is locked, and the goats bounce for joy. Pure so it can be unit-tested.
 */
object ChaseScene {
    const val CYCLE_MILLIS = 8600
    const val UNITS_WIDE = 10f
    val POINTS = listOf(1f, 3f, 5f, 7f, 9f)

    private const val FADE_IN_END = 500f
    private const val FADE_OUT_START = 7700f
    private const val FADE_OUT_END = 8300f

    private const val TIGER_START = 9f
    private const val TIGER_LANDING = 5f
    private const val CAPTURED_AT = 7f
    private const val CROUCH_START = 2200f
    private const val LEAP_START = 2500f
    private const val LEAP_END = 3150f
    private const val LAND_END = 3450f
    private const val LEAP_HEIGHT = 2.4f
    private const val SHAKE_START = 4650f
    private const val SHAKE_END = 5150f
    const val TRAP_START = 4950f
    private const val LOCK_POP_MILLIS = 350f
    private const val POOF_MILLIS = 420f
    private const val BURST_MILLIS = 650f
    private const val CHEER_START = 5250f
    private const val CHEER_END = 7500f
    private const val CHEER_PERIOD = 620f

    /** A goat hopping point to point. */
    private class Walk(val start: Float, val from: Float, val to: Float) {
        private val hops = (abs(to - from) / 2).roundToInt()
        val end = start + hops * HOP_MILLIS

        fun x(t: Float): Float {
            val p = ((t - start) / (end - start)).coerceIn(0f, 1f)
            return from + (to - from) * p
        }

        fun lift(t: Float): Float {
            if (t <= start || t >= end) return 0f
            val hop = ((t - start) / HOP_MILLIS) % 1f
            return sin(PI * hop).toFloat() * HOP_HEIGHT
        }

        companion object {
            const val HOP_MILLIS = 330f
            const val HOP_HEIGHT = 0.7f
        }
    }

    // The captured goat goes furthest; the others fill in behind it.
    private val captured = Walk(start = 300f, from = -1f, to = CAPTURED_AT)
    private val cheering = listOf(
        Walk(start = 900f, from = -1f, to = 3f),
        Walk(start = 1600f, from = -1f, to = 1f),
        Walk(start = 3500f, from = 11f, to = 7f),
        Walk(start = 4200f, from = 11f, to = 9f),
    )

    /** Where everyone stands once the tiger is locked in. */
    val TRAPPED_GOATS: List<Float> = cheering.map { it.to }
    const val TRAPPED_TIGER: Float = TIGER_LANDING

    /** Shown instead of the loop when system animations are off. */
    val STILL_FRAME: ChaseFrame get() = frameAt(CHEER_START - 1f)

    fun frameAt(millis: Float): ChaseFrame {
        val t = millis.mod(CYCLE_MILLIS.toFloat())
        val sprites = buildList {
            capturedGoat(t)?.let(::add)
            cheering.forEachIndexed { index, walk -> add(goat(walk, index, t)) }
            add(tiger(t))
        }
        val burstProgress = (t - poofStart) / BURST_MILLIS
        val lockProgress = ((t - TRAP_START) / LOCK_POP_MILLIS).coerceIn(0f, 1f)
        return ChaseFrame(
            sprites = sprites,
            burstX = CAPTURED_AT.takeIf { burstProgress in 0f..1f },
            burstProgress = burstProgress.coerceIn(0f, 1f),
            // Pops up with a little overshoot.
            lockScale = lockProgress * (1f + 0.35f * sin(PI * lockProgress).toFloat()),
            trapMillis = (t - TRAP_START).coerceAtLeast(0f),
            alpha = sceneAlpha(t),
        )
    }

    /** The tiger passes over the captured goat halfway through its leap. */
    private val poofStart = (LEAP_START + LEAP_END) / 2

    private fun capturedGoat(t: Float): Sprite? {
        if (t < captured.start) return null
        val poof = (t - poofStart) / POOF_MILLIS
        if (poof >= 1f) return null
        val p = poof.coerceAtLeast(0f)
        return Sprite(
            Actor.GOAT,
            x = captured.x(t),
            lift = captured.lift(t) + 0.6f * p,
            scaleX = 1f + 0.4f * p,
            scaleY = 1f + 0.4f * p,
            rotation = 80f * p,
            alpha = 1f - p,
        )
    }

    private fun goat(walk: Walk, index: Int, t: Float): Sprite {
        val cheer = if (t in CHEER_START..CHEER_END) {
            val phase = ((t - CHEER_START - index * 150f) / CHEER_PERIOD).coerceAtLeast(0f) % 1f
            sin(PI * phase).toFloat() * 0.35f
        } else {
            0f
        }
        return Sprite(Actor.GOAT, x = walk.x(t), lift = walk.lift(t) + cheer)
    }

    private fun tiger(t: Float): Sprite {
        // A slow breath while it waits.
        val breath = sin(2 * PI * t / 1600f).toFloat() * 0.03f
        return when {
            t < CROUCH_START -> Sprite(Actor.TIGER, TIGER_START, scaleX = 1f - breath, scaleY = 1f + breath)
            t < LEAP_START -> {
                val p = (t - CROUCH_START) / (LEAP_START - CROUCH_START)
                Sprite(Actor.TIGER, TIGER_START, scaleX = 1f + 0.12f * p, scaleY = 1f - 0.2f * p)
            }
            t < LEAP_END -> {
                val p = (t - LEAP_START) / (LEAP_END - LEAP_START)
                val arc = sin(PI * p).toFloat()
                Sprite(
                    Actor.TIGER,
                    x = TIGER_START + (TIGER_LANDING - TIGER_START) * easeInOut(p),
                    lift = arc * LEAP_HEIGHT,
                    scaleX = 1f + 0.18f * arc,
                    scaleY = 1f + 0.18f * arc,
                    rotation = -14f * sin(2 * PI * p).toFloat(),
                )
            }
            t < LAND_END -> {
                val p = (t - LEAP_END) / (LAND_END - LEAP_END)
                val squash = sin(PI * p).toFloat()
                Sprite(Actor.TIGER, TIGER_LANDING, scaleX = 1f + 0.14f * squash, scaleY = 1f - 0.2f * squash)
            }
            t in SHAKE_START..SHAKE_END -> {
                val p = (t - SHAKE_START) / (SHAKE_END - SHAKE_START)
                Sprite(Actor.TIGER, TIGER_LANDING, rotation = 11f * sin(6 * PI * p).toFloat() * (1f - p))
            }
            else -> Sprite(Actor.TIGER, TIGER_LANDING, scaleX = 1f - breath, scaleY = 1f + breath)
        }
    }

    private fun sceneAlpha(t: Float): Float = when {
        t < FADE_IN_END -> t / FADE_IN_END
        t < FADE_OUT_START -> 1f
        t < FADE_OUT_END -> 1f - (t - FADE_OUT_START) / (FADE_OUT_END - FADE_OUT_START)
        else -> 0f
    }

    private fun easeInOut(p: Float): Float = (1f - cos(PI * p).toFloat()) / 2f
}
