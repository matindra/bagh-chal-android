package com.matinstudios.baghchal.ui.board

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.matinstudios.baghchal.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val LineColor = Color(0xFF6B4423)
private val ShadowColor = Color(0x33000000)
private val BurstColor = Color(0xFFFFB347)
private val PuffColor = Color(0xFFFFFFFF)
private val TrapColor = Color(0xFFFFD54F)

private const val SPARKS = 10
private const val TRAP_PULSE_MILLIS = 900f

/** Sky and hill colours behind the chase scene; hills are drawn a little see-through. */
class ChaseSceneryColors(val skyTop: Color, val skyBottom: Color, val farHill: Color, val nearHill: Color)

/**
 * Decorative looping scene for the home screen: tiger and goats acting out the game on a
 * stretch of board line. Not interactive; hidden from accessibility. With system animations
 * off it shows one still moment. [scenery] defaults to the board-tinted theme colours.
 */
@Composable
fun ChaseBanner(modifier: Modifier = Modifier, scenery: ChaseSceneryColors? = null) {
    val animationsOn = rememberAnimationsOn()
    val clock by rememberInfiniteTransition(label = "chase").animateFloat(
        initialValue = 0f,
        targetValue = ChaseScene.CYCLE_MILLIS.toFloat(),
        animationSpec = infiniteRepeatable(tween(ChaseScene.CYCLE_MILLIS, easing = LinearEasing), RepeatMode.Restart),
        label = "chaseClock",
    )
    val tiger = painterResource(LocalPieceArt.current.tiger)
    val goat = painterResource(LocalPieceArt.current.goat)
    val lock = painterResource(R.drawable.lock_badge)
    val colors = MaterialTheme.colorScheme
    val sky = scenery?.let { listOf(it.skyTop, it.skyBottom) }
        ?: listOf(colors.tertiaryContainer.copy(alpha = 0.75f), colors.surfaceContainerLow)
    val nearHill = scenery?.nearHill ?: colors.secondaryContainer
    val farHill = scenery?.farHill ?: colors.tertiaryContainer

    Canvas(modifier) {
        val frame = if (animationsOn) ChaseScene.frameAt(clock) else ChaseScene.STILL_FRAME
        val unit = size.width / ChaseScene.UNITS_WIDE
        val piece = minOf(unit * 1.45f, size.height * 0.36f)
        val groundY = size.height * 0.74f
        fun px(x: Float) = x * unit

        drawRect(Brush.verticalGradient(sky))
        drawHills(farHill.copy(alpha = 0.7f), nearHill.copy(alpha = 0.9f), groundY)
        drawBoardStrip(groundY, unit, piece)

        val alpha = frame.alpha
        val tigerSprite = frame.sprites.first { it.actor == Actor.TIGER }

        // Trapped tiger: a soft gold glow behind it.
        if (frame.lockScale > 0f) {
            val pulse = (sin(2 * PI * frame.trapMillis / TRAP_PULSE_MILLIS).toFloat() + 1f) / 2f
            val center = Offset(px(tigerSprite.x), groundY)
            val r = piece * (0.85f + 0.12f * pulse)
            drawCircle(
                Brush.radialGradient(listOf(TrapColor.copy(alpha = 0.6f * alpha), Color.Transparent), center, r),
                radius = r,
                center = center,
            )
            drawCircle(
                TrapColor.copy(alpha = (0.55f + 0.45f * pulse) * alpha * frame.lockScale.coerceAtMost(1f)),
                radius = piece * 0.6f,
                center = center,
                style = Stroke(width = piece * 0.06f),
            )
        }

        for (sprite in frame.sprites) {
            val shrink = 1f - (sprite.lift / 3f).coerceIn(0f, 0.6f)
            drawOval(
                ShadowColor.copy(alpha = ShadowColor.alpha * alpha * sprite.alpha),
                topLeft = Offset(px(sprite.x) - piece * 0.38f * shrink, groundY + piece * 0.38f),
                size = Size(piece * 0.76f * shrink, piece * 0.16f * shrink),
            )
        }

        frame.burstX?.let { drawBurst(Offset(px(it), groundY), piece, frame.burstProgress, alpha) }

        // Tiger last, so it sails over the goats when it leaps.
        for (sprite in frame.sprites.sortedBy { it.actor == Actor.TIGER }) {
            // Never higher than the banner's top edge, even at the top of the leap.
            val y = maxOf(groundY - sprite.lift * unit, piece * 0.72f)
            drawSprite(if (sprite.actor == Actor.TIGER) tiger else goat, sprite, px(sprite.x), y, piece, alpha)
        }

        if (frame.lockScale > 0f) {
            val size = piece * 0.42f * frame.lockScale
            val at = Offset(px(tigerSprite.x) + piece * 0.36f, groundY - piece * 0.36f)
            drawPainter(lock, at, size, alpha)
        }
    }
}

/** Two rolling hills behind the line. */
private fun DrawScope.drawHills(far: Color, near: Color, groundY: Float) {
    val w = size.width
    val h = size.height
    val farPath = Path().apply {
        moveTo(0f, groundY - h * 0.22f)
        cubicTo(w * 0.25f, groundY - h * 0.5f, w * 0.45f, groundY - h * 0.1f, w * 0.65f, groundY - h * 0.32f)
        cubicTo(w * 0.8f, groundY - h * 0.48f, w * 0.92f, groundY - h * 0.3f, w, groundY - h * 0.36f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(farPath, far)
    val nearPath = Path().apply {
        moveTo(0f, groundY - h * 0.06f)
        cubicTo(w * 0.3f, groundY - h * 0.2f, w * 0.6f, groundY + h * 0.02f, w, groundY - h * 0.14f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(nearPath, near)
}

/**
 * A stretch of board: the line the pieces stand on, with two more rows of the board fading
 * above it. Diagonals run from every other point, as on the real board, so they cross in X's.
 */
private fun DrawScope.drawBoardStrip(groundY: Float, unit: Float, piece: Float) {
    val points = ChaseScene.POINTS
    val first = points.first() * unit
    val last = points.last() * unit
    val rowGap = piece * 0.95f
    val stroke = piece * 0.045f
    for (row in 1..2) {
        val lower = groundY - (row - 1) * rowGap
        val upper = groundY - row * rowGap
        val faint = LineColor.copy(alpha = if (row == 1) 0.24f else 0.12f)
        drawLine(faint, Offset(first, upper), Offset(last, upper), strokeWidth = stroke)
        points.forEachIndexed { index, x ->
            drawLine(faint, Offset(x * unit, lower), Offset(x * unit, upper), strokeWidth = stroke)
            // Points with an even (row + column) have diagonals; the bottom row is row 0 here.
            if ((row - 1 + index) % 2 == 0) {
                if (index > 0) drawLine(faint, Offset(x * unit, lower), Offset((x - 2) * unit, upper), strokeWidth = stroke)
                if (index < points.lastIndex) drawLine(faint, Offset(x * unit, lower), Offset((x + 2) * unit, upper), strokeWidth = stroke)
            }
        }
    }
    drawLine(LineColor.copy(alpha = 0.75f), Offset(first, groundY), Offset(last, groundY), strokeWidth = stroke * 1.6f, cap = StrokeCap.Round)
    for (x in points) drawCircle(LineColor, radius = stroke * 2f, center = Offset(x * unit, groundY))
}

private fun DrawScope.drawSprite(painter: Painter, sprite: Sprite, x: Float, y: Float, piece: Float, sceneAlpha: Float) {
    val a = sprite.alpha * sceneAlpha
    if (a <= 0f) return
    val foot = Offset(x, y + piece / 2)
    rotate(sprite.rotation, pivot = Offset(x, y)) {
        scale(sprite.scaleX, sprite.scaleY, pivot = foot) {
            drawPainter(painter, Offset(x, y), piece, a)
        }
    }
    if (sprite.actor == Actor.GOAT && sprite.alpha < 1f) {
        // The captured goat vanishes in a puff.
        val p = 1f - sprite.alpha
        drawCircle(PuffColor.copy(alpha = 0.7f * sprite.alpha * sceneAlpha), radius = piece * (0.35f + 0.5f * p), center = Offset(x, y))
    }
}

/** Sparks flying out where a goat was captured. */
private fun DrawScope.drawBurst(center: Offset, piece: Float, t: Float, alpha: Float) {
    val fade = (1f - t) * alpha
    for (i in 0 until SPARKS) {
        val angle = 2 * PI * i / SPARKS + 0.3
        val dir = Offset(cos(angle).toFloat(), sin(angle).toFloat())
        val start = center + dir * (piece * (0.3f + 0.5f * t))
        val end = center + dir * (piece * (0.45f + 0.8f * t))
        drawLine(BurstColor.copy(alpha = fade), start, end, strokeWidth = piece * 0.07f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawPainter(painter: Painter, center: Offset, size: Float, alpha: Float) {
    if (size <= 0f || alpha <= 0f) return
    translate(center.x - size / 2, center.y - size / 2) {
        with(painter) { draw(Size(size, size), alpha = alpha) }
    }
}
