package com.matinstudios.baghchal.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matinstudios.baghchal.ui.board.rememberAnimationsOn

private val TileShape = RoundedCornerShape(14.dp)
private val ChipSize = 32.dp

/** The darker lip under the tile face; pressing sinks the face into it. */
private val EdgeHeight = 4.dp
private const val BOB_MILLIS = 1300
private const val SHEEN_MILLIS = 5500

/** Where in its cycle the sheen starts sweeping across (the rest of the time it is off the tile). */
private const val SHEEN_START = 0.7f

/**
 * A compact raised mode tile: a line icon on a floating chip that bobs, the title and detail beside it,
 * and a sheen that sweeps by now and then. [phase] (0..3) keeps neighbouring tiles out of step.
 */
@Composable
fun ModeTile(
    @StringRes title: Int,
    @StringRes detail: Int,
    @DrawableRes icon: Int,
    container: Color,
    content: Color,
    accent: Color,
    onAccent: Color,
    phase: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val sink by animateDpAsState(if (pressed) EdgeHeight - 1.dp else 0.dp, label = "tileSink")
    val edge = lerp(container, Color.Black, 0.2f)
    val motion = rememberTileMotion(phase)

    Box(
        modifier
            .shadow(4.dp, TileShape, ambientColor = edge, spotColor = edge)
            .background(edge, TileShape)
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(top = sink, bottom = EdgeHeight - sink),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .heightIn(min = 52.dp)
                .clip(TileShape)
                .background(container)
                .indication(interaction, ripple())
                .drawWithContent {
                    drawContent()
                    drawSheen(motion.sheen())
                }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GlyphChip(icon, accent, onAccent, content, motion.bob)
            Column(Modifier.weight(1f)) {
                // One line on every screen: shrinks a little before it would wrap.
                BasicText(
                    stringResource(title),
                    style = MaterialTheme.typography.titleSmall.copy(color = content, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = 14.sp),
                )
                BasicText(
                    stringResource(detail),
                    style = MaterialTheme.typography.bodySmall.copy(color = content.copy(alpha = 0.8f)),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 11.sp),
                )
            }
        }
    }
}

private class TileMotion(val bob: () -> Float, val sheen: () -> Float)

@Composable
private fun rememberTileMotion(phase: Int): TileMotion {
    if (!rememberAnimationsOn()) return remember { TileMotion({ 0f }, { 0f }) }
    val transition = rememberInfiniteTransition(label = "tile")
    val bob = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(BOB_MILLIS, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
            StartOffset(phase * 650, StartOffsetType.FastForward),
        ),
        label = "tileBob",
    )
    val sheen = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(SHEEN_MILLIS, easing = LinearEasing),
            RepeatMode.Restart,
            StartOffset(phase * 1400, StartOffsetType.FastForward),
        ),
        label = "tileSheen",
    )
    return remember(bob, sheen) { TileMotion({ bob.value }, { sheen.value }) }
}

/**
 * The card's single-colour line icon on a solid [accent] chip with a darker lip. The chip floats over a
 * ground shadow: it rises and the shadow shrinks as [bob] goes to 1.
 */
@Composable
private fun GlyphChip(@DrawableRes icon: Int, accent: Color, onAccent: Color, shadow: Color, bob: () -> Float) {
    val lip = lerp(accent, Color.Black, 0.35f)
    Box(
        Modifier
            .size(ChipSize)
            .drawBehind {
                val b = bob()
                val w = 22.dp.toPx() * (1f - 0.22f * b)
                drawOval(
                    shadow.copy(alpha = 0.24f - 0.12f * b),
                    topLeft = Offset((size.width - w) / 2, size.height - 1.dp.toPx()),
                    size = Size(w, 5.dp.toPx()),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(ChipSize)
                .graphicsLayer { translationY = -3.dp.toPx() * bob() }
                .drawBehind {
                    val radius = CornerRadius(10.dp.toPx())
                    drawRoundRect(lip, topLeft = Offset(0f, 3.dp.toPx()), cornerRadius = radius)
                    drawRoundRect(accent, cornerRadius = radius)
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = onAccent, modifier = Modifier.size(20.dp))
        }
    }
}

/** A slanted band of light crossing the tile during the last part of its cycle. */
private fun DrawScope.drawSheen(t: Float) {
    if (t < SHEEN_START) return
    val p = (t - SHEEN_START) / (1f - SHEEN_START)
    val band = size.width * 0.3f
    val slant = size.height * 0.5f
    val x = -band - slant + p * (size.width + 2 * band + slant)
    val path = Path().apply {
        moveTo(x + slant, 0f)
        lineTo(x + slant + band, 0f)
        lineTo(x + band, size.height)
        lineTo(x, size.height)
        close()
    }
    drawPath(path, Color.White.copy(alpha = 0.28f))
}
