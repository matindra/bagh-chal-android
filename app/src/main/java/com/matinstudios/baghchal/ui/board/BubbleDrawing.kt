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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

// Green-tinted water bubble: clear middle, deeper colour at the edge, white shine on top.
private val BubbleCore = Color(0x33E8FFF4)
private val BubbleMid = Color(0x664CC38A)
private val BubbleEdge = Color(0xCC1E8E5A)
private val BubbleRim = Color(0xB3FFFFFF)
private val BubbleOutline = Color(0x80104A30)
private val BubbleShadow = Color(0x40000000)
private val Shine = Color(0xE6FFFFFF)

private const val BUBBLE_RADIUS = 26f

/** 0..1 back and forth, for the bubbles' gentle bob. */
@Composable
fun rememberBubbleBob(): State<Float> = rememberInfiniteTransition(label = "bubble").animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
    label = "bob",
)

/** A legal-move marker at [center]; [scale] is the board's px per art unit, [bob] is 0..1. */
fun DrawScope.drawMoveBubble(center: Offset, scale: Float, bob: Float) {
    val r = BUBBLE_RADIUS * scale * (1f + 0.05f * bob)
    val c = center + Offset(0f, -3f * scale * bob)

    // Soft shadow on the board, staying put while the bubble lifts.
    val shadowCenter = center + Offset(0f, r * 0.55f)
    drawOval(
        Brush.radialGradient(listOf(BubbleShadow, Color.Transparent), shadowCenter, r * 0.9f),
        topLeft = shadowCenter - Offset(r * 0.9f, r * 0.35f),
        size = Size(r * 1.8f, r * 0.7f),
    )

    // Body: clear in the middle, coloured at the edge, lit from the top-left.
    drawCircle(
        Brush.radialGradient(
            0f to BubbleCore,
            0.65f to BubbleMid,
            1f to BubbleEdge,
            center = c + Offset(-r * 0.2f, -r * 0.2f),
            radius = r * 1.2f,
        ),
        radius = r,
        center = c,
    )
    drawCircle(BubbleOutline, radius = r, center = c, style = Stroke(width = 1.5f * scale))
    drawCircle(BubbleRim, radius = r - 2f * scale, center = c, style = Stroke(width = 1.5f * scale))

    // Shine: a tilted oval near the top-left, and a small glint at the bottom-right.
    val shine = c + Offset(-r * 0.35f, -r * 0.42f)
    rotate(-35f, pivot = shine) {
        drawOval(
            Brush.radialGradient(listOf(Shine, Shine.copy(alpha = 0f)), shine, r * 0.4f),
            topLeft = shine - Offset(r * 0.38f, r * 0.2f),
            size = Size(r * 0.76f, r * 0.4f),
        )
    }
    drawCircle(Shine.copy(alpha = 0.55f), radius = r * 0.1f, center = c + Offset(r * 0.45f, r * 0.4f))
}
