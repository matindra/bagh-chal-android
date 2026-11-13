package com.matinstudios.baghchal.ui.board

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

// "Forest": a green dot sending out a fading lighter-green ripple.
private val DotColor = Color(0xFF2E9E62)
private val RippleColor = Color(0xFF50D78C)

private const val DOT_RADIUS = 13f
private const val RIPPLE_START = 14f
private const val RIPPLE_GROWTH = 26f
private const val RIPPLE_WIDTH = 4.5f
private const val RIPPLE_ALPHA = 0.9f
private const val RIPPLE_MILLIS = 1600

// Each dot starts its ripple a little after the one before, so they don't pulse as one.
private const val STAGGER = 0.13f

// Where a ripple rests when system animations are off.
private const val STILL_PHASE = 0.4f
private const val HOVER_SCALE = 1.35f

/** 0..1, one ripple every 1.6 s; stays still when system animations are off. */
@Composable
fun rememberRipplePhase(): State<Float> {
    if (!rememberAnimationsOn()) return remember { mutableFloatStateOf(STILL_PHASE) }
    return rememberInfiniteTransition(label = "ripple").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(RIPPLE_MILLIS, easing = LinearEasing)),
        label = "phase",
    )
}

/**
 * A legal-move marker at [center]; [scale] is the board's px per art unit, [phase] is 0..1 from
 * [rememberRipplePhase] and [index] the marker's place in the list (staggers the ripples).
 * [hovered]: a carried piece is over it, so it swells.
 */
fun DrawScope.drawMoveBubble(center: Offset, scale: Float, phase: Float, index: Int, hovered: Boolean = false) {
    val grow = scale * (if (hovered) HOVER_SCALE else 1f)
    val ripple = if (phase == STILL_PHASE) phase else (phase + index * STAGGER) % 1f
    drawCircle(
        RippleColor.copy(alpha = (1f - ripple) * RIPPLE_ALPHA),
        radius = (RIPPLE_START + RIPPLE_GROWTH * ripple) * grow,
        center = center,
        style = Stroke(width = RIPPLE_WIDTH * scale),
    )
    drawCircle(DotColor, radius = DOT_RADIUS * grow, center = center)
}
