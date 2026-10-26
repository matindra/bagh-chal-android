package com.matinstudios.baghchal.ui.board

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

// A plain, flat green dot: no outline, shine or shadow.
private val DotColor = Color(0xEB2E8B57)

private const val DOT_RADIUS = 17f

/** 0..1 back and forth, for the markers' gentle breathe. */
@Composable
fun rememberBubbleBob(): State<Float> = rememberInfiniteTransition(label = "bubble").animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
    label = "bob",
)

/** A legal-move marker at [center]; [scale] is the board's px per art unit, [bob] is 0..1. */
fun DrawScope.drawMoveBubble(center: Offset, scale: Float, bob: Float) {
    // A gentle breathe in size; no movement.
    val r = DOT_RADIUS * scale * (1f + 0.08f * bob)
    drawCircle(DotColor, radius = r, center = center)
}
