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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.matinstudios.baghchal.engine.Side
import kotlin.math.PI

// Two colours take turns around the ring: tigers coral and navy, goats dark green and snow.
private val TigerColors = Color(0xFFF0552F) to Color(0xFF0A1636)
private val GoatColors = Color(0xFF1F6B45) to Color(0xFFF4F8FF)

private const val DASHES = 12
private const val DASH_SHARE = 0.55f
private const val RING_WIDTH = 7f
private const val TURN_MILLIS = 4000

/** Degrees 0..360, one turn every 4 s; stays at 0 when system animations are off. */
@Composable
fun rememberRingTurn(): State<Float> {
    if (!rememberAnimationsOn()) return remember { mutableFloatStateOf(0f) }
    return rememberInfiniteTransition(label = "ring").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(TURN_MILLIS, easing = LinearEasing)),
        label = "turn",
    )
}

/** Degrees for a ring driven by an existing clock in [millis] (one turn every 4 s). */
fun ringTurnAt(millis: Float): Float = millis % TURN_MILLIS / TURN_MILLIS * 360f

/**
 * The turning ring used for a selected piece, a hinted placement and Home's trapped tiger:
 * [DASHES] rounded dashes alternating between [side]'s two colours, turned by [degrees].
 * [scale] is px per board art unit (a piece is 90 units).
 */
fun DrawScope.drawPieceRing(center: Offset, radius: Float, side: Side, scale: Float, degrees: Float, alpha: Float = 1f) {
    val width = RING_WIDTH * scale
    val segment = (2 * PI * radius).toFloat() / DASHES
    // Round caps add half the width at each end, so the drawn dash is shortened to keep the gap.
    val dash = (segment * DASH_SHARE - width).coerceAtLeast(0.1f)
    val (first, second) = if (side == Side.TIGER) TigerColors else GoatColors
    rotate(degrees, center) {
        listOf(first to 0f, second to segment).forEach { (color, phase) ->
            drawCircle(
                color,
                alpha = alpha,
                radius = radius,
                center = center,
                style = Stroke(
                    width = width,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, 2 * segment - dash), phase),
                ),
            )
        }
    }
}
