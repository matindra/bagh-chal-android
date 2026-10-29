package com.matinstudios.baghchal.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matinstudios.baghchal.R
import kotlin.math.PI
import kotlin.math.cos

/**
 * A chunky game button: a [face] sitting on a darker [lip]. Pressing sinks the face into the lip.
 * The lip is drawn only where the face doesn't cover it, so the face may be see-through.
 * [faceModifier] sizes the face.
 */
@Composable
internal fun RaisedButton(
    onClick: () -> Unit,
    face: Color,
    lip: Color,
    shape: Shape,
    lipHeight: Dp,
    modifier: Modifier = Modifier,
    faceModifier: Modifier = Modifier,
    border: Color? = null,
    enabled: Boolean = true,
    decoration: (DrawScope.() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val sink by animateDpAsState(if (pressed) lipHeight - 1.dp else 0.dp, label = "raisedSink")
    // Min constraints pass through, so a width given to [modifier] (fillMaxWidth, weight) reaches the face.
    Box(
        modifier
            .drawBehind { drawLip(shape, lip, sink.toPx(), (lipHeight - sink).toPx()) }
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(top = sink, bottom = lipHeight - sink),
        propagateMinConstraints = true,
    ) {
        Box(
            faceModifier
                .clip(shape)
                .background(face)
                .then(if (decoration != null) Modifier.drawBehind(decoration) else Modifier)
                .then(if (border != null) Modifier.border(1.5.dp, border, shape) else Modifier)
                .indication(interaction, ripple()),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

/** The [shape] filled with [color] over the whole button, minus the face inset by [top] and [bottom]. */
private fun DrawScope.drawLip(shape: Shape, color: Color, top: Float, bottom: Float) {
    val lip = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawLip)) }
    val faceSize = Size(size.width, (size.height - top - bottom).coerceAtLeast(0f))
    val face = Path().apply {
        addOutline(shape.createOutline(faceSize, layoutDirection, this@drawLip))
        translate(Offset(0f, top))
    }
    drawPath(Path().apply { op(lip, face, PathOperation.Difference) }, color)
}

/** Square icon button for Home's top corners: gold for the Shop, frosted for the rest. */
@Composable
internal fun HomeIconButton(
    @DrawableRes icon: Int,
    @StringRes label: Int,
    onClick: () -> Unit,
    colors: HomeIconColors = HomeIconColors.Frost,
) {
    RaisedButton(
        onClick = onClick,
        face = colors.face,
        lip = colors.lip,
        shape = RoundedCornerShape(16.dp),
        lipHeight = 4.dp,
        faceModifier = Modifier.size(48.dp),
        border = colors.border,
    ) {
        Icon(
            painterResource(icon),
            contentDescription = stringResource(label),
            tint = colors.icon,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** Face, lip and icon colours of a [HomeIconButton]; only frosted glass has a border. */
internal data class HomeIconColors(val face: Color, val lip: Color, val icon: Color, val border: Color? = null) {
    companion object {
        val Frost = HomeIconColors(HomeNight.Frost, HomeNight.FrostLip, HomeNight.OnFrost, HomeNight.FrostBorder)
        val Gold = HomeIconColors(HomeNight.Gold, HomeNight.GoldLip, HomeNight.OnGold)
        val Cyan = HomeIconColors(HomeNight.Cyan, HomeNight.CyanLip, HomeNight.OnCyan)
        val Violet = HomeIconColors(HomeNight.Violet, HomeNight.VioletLip, HomeNight.OnViolet)
        val Coral = HomeIconColors(HomeNight.Coral, HomeNight.CoralLip, Color.White)
    }
}

/** One shine wave runs down Home's mode buttons every [SHINE_MILLIS], each [SHINE_STAGGER] after the one above. */
private const val SHINE_MILLIS = 4200
private const val SHINE_STAGGER = 220

/** How much coral shows in the Play with AI glass. */
private const val PLAY_AI_GLASS = 0.5f

/** Part of the cycle the shine spends crossing; the rest it waits off the button. */
private const val SHINE_ACTIVE = 0.38f

/** The buttons bob [FLOAT_DP] up and back every [FLOAT_MILLIS], each a quarter cycle apart. */
private const val FLOAT_MILLIS = 3400
private const val FLOAT_DP = 3f

/** Gentle up-and-down bob for the Home button at [index] (0 = top). Still when animations are off. */
private fun Modifier.iceFloat(clock: State<Long>, index: Int): Modifier = graphicsLayer {
    val phase = clock.value.toDouble() / FLOAT_MILLIS + index * 0.25
    translationY = -FLOAT_DP.dp.toPx() * ((1 - cos(2 * PI * phase)) / 2).toFloat()
}

/** Shine for the Home button at [index]: the wave's progress through its cycle, or no shine yet. */
private fun DrawScope.drawShineWave(clock: State<Long>, index: Int) {
    val t = clock.value - index * SHINE_STAGGER
    if (t < 0) return
    drawShine((t % SHINE_MILLIS).toFloat() / SHINE_MILLIS)
}

/** The main action: big semi-transparent coral "Play with AI", floating, with the shine wave starting here. */
@Composable
internal fun PlayAiButton(clock: State<Long>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    RaisedButton(
        onClick = onClick,
        // Semi-transparent coral glass (the snow shows through), stronger than the mode rows
        // so it stays the main action.
        face = HomeNight.Coral.copy(alpha = PLAY_AI_GLASS),
        lip = HomeNight.CoralLip.copy(alpha = PLAY_AI_GLASS),
        shape = RoundedCornerShape(26.dp),
        border = HomeNight.Coral.copy(alpha = 0.8f),
        lipHeight = 7.dp,
        modifier = modifier.iceFloat(clock, 0),
        faceModifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
        decoration = {
            drawRect(Color.White.copy(alpha = 0.3f), size = Size(size.width, 5.dp.toPx()))
            drawShineWave(clock, 0)
        },
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .size(50.dp)
                    .drawBehind { drawCircle(HomeNight.CoralLip, center = center + Offset(0f, 4.dp.toPx())) }
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_play), contentDescription = null, tint = HomeNight.Coral, modifier = Modifier.size(26.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasicText(
                    stringResource(R.string.mode_vs_ai),
                    style = TextStyle(
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        shadow = Shadow(HomeNight.CoralLip, with(LocalDensity.current) { Offset(0f, 2.dp.toPx()) }, 0f),
                    ),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 18.sp, maxFontSize = 27.sp),
                )
                // Small text sits on a white pill: white on coral is only readable when large.
                BasicText(
                    stringResource(R.string.mode_vs_ai_detail),
                    style = TextStyle(color = HomeNight.CoralInk, fontWeight = FontWeight.Black, fontSize = 12.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .background(Color.White, CircleShape)
                        .padding(horizontal = 9.dp, vertical = 1.dp),
                )
            }
        }
    }
}

/** A slanted white band crossing left to right in the first part of its cycle [t] (0..1). */
private fun DrawScope.drawShine(t: Float) {
    if (t >= SHINE_ACTIVE) return
    val p = FastOutSlowInEasing.transform(t / SHINE_ACTIVE)
    val band = 44.dp.toPx()
    val slant = size.height * 0.36f // about 20°
    val x = -band - slant + p * (size.width + 2 * band + slant)
    val path = Path().apply {
        moveTo(x + slant, 0f)
        lineTo(x + slant + band, 0f)
        lineTo(x + band, size.height)
        lineTo(x, size.height)
        close()
    }
    drawPath(path, Color.White.copy(alpha = 0.45f))
}

/** A mode's colours: the icon chip, its darker lip, and the light text tint on its glass row. */
internal class ModeColors(val chip: Color, val chipLip: Color, val ink: Color)

/**
 * A tinted-glass mode row: the mode's colour, see-through over the night sky, with its icon chip,
 * title and detail, and a chevron. [index] places it in the float and shine wave (Play with AI is 0).
 */
@Composable
internal fun FrostModeButton(
    @StringRes title: Int,
    @StringRes detail: Int,
    @DrawableRes icon: Int,
    colors: ModeColors,
    clock: State<Long>,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chip = colors.chip
    val chipLip = colors.chipLip
    RaisedButton(
        onClick = onClick,
        // The chip colour at glass strength: the sky and snow show through.
        face = chip.copy(alpha = 0.16f),
        lip = HomeNight.FrostLip,
        shape = RoundedCornerShape(18.dp),
        lipHeight = 5.dp,
        modifier = modifier.iceFloat(clock, index),
        faceModifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        border = chip.copy(alpha = 0.5f),
        decoration = { drawShineWave(clock, index) },
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .drawBehind {
                        val r = CornerRadius(12.dp.toPx())
                        drawRoundRect(chipLip, Offset(0f, 3.dp.toPx()), size, r)
                        drawRoundRect(chip, cornerRadius = r)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                BasicText(
                    stringResource(title),
                    style = TextStyle(color = HomeNight.OnFrost, fontWeight = FontWeight.Black),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 13.sp, maxFontSize = 18.sp),
                )
                BasicText(
                    stringResource(detail),
                    style = TextStyle(color = colors.ink, fontWeight = FontWeight.ExtraBold),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 12.sp),
                )
            }
            Icon(
                painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = HomeNight.OnFrostSoft,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** Gold pill button (Resume). */
@Composable
internal fun GoldPillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    RaisedButton(
        onClick = onClick,
        face = HomeNight.Gold,
        lip = HomeNight.GoldLip,
        shape = RoundedCornerShape(16.dp),
        lipHeight = 4.dp,
        modifier = modifier,
        faceModifier = Modifier.heightIn(min = 44.dp),
    ) {
        BasicText(
            text,
            style = TextStyle(color = HomeNight.OnGold, fontWeight = FontWeight.Black, fontSize = 16.sp),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 18.dp),
        )
    }
}
