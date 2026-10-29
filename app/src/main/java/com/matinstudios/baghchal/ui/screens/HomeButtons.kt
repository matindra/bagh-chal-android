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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
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

/**
 * A chunky game button: a [face] sitting on a darker [lip]. Pressing sinks the face into the lip.
 * The face must be opaque (the lip runs underneath it). [faceModifier] sizes the face.
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
    decoration: (DrawScope.() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val sink by animateDpAsState(if (pressed) lipHeight - 1.dp else 0.dp, label = "raisedSink")
    Box(
        modifier
            .background(lip, shape)
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(top = sink, bottom = lipHeight - sink),
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

/** Square icon button for Home's top corners: gold for the Shop, frosted for the rest. */
@Composable
internal fun HomeIconButton(
    @DrawableRes icon: Int,
    @StringRes label: Int,
    onClick: () -> Unit,
    gold: Boolean = false,
) {
    RaisedButton(
        onClick = onClick,
        face = if (gold) HomeNight.Gold else HomeNight.Frost,
        lip = if (gold) HomeNight.GoldLip else HomeNight.FrostLip,
        shape = RoundedCornerShape(16.dp),
        lipHeight = 4.dp,
        faceModifier = Modifier.size(48.dp),
        border = if (gold) null else HomeNight.FrostBorder,
    ) {
        Icon(
            painterResource(icon),
            contentDescription = stringResource(label),
            tint = if (gold) HomeNight.OnGold else HomeNight.OnFrost,
            modifier = Modifier.size(24.dp),
        )
    }
}

private const val SHINE_MILLIS = 2800

/** Part of the cycle the shine spends crossing; the rest it waits off the button. */
private const val SHINE_ACTIVE = 0.55f

/** The main action: big cyan "Play with AI" with a shine sweeping across now and then. */
@Composable
internal fun PlayAiButton(clock: State<Long>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    RaisedButton(
        onClick = onClick,
        face = HomeNight.Cyan,
        lip = HomeNight.CyanLip,
        shape = RoundedCornerShape(26.dp),
        lipHeight = 7.dp,
        modifier = modifier,
        faceModifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
        decoration = {
            drawRect(Color.White.copy(alpha = 0.4f), size = Size(size.width, 5.dp.toPx()))
            drawShine((clock.value % SHINE_MILLIS).toFloat() / SHINE_MILLIS)
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
                    .drawBehind { drawCircle(HomeNight.CyanLip, center = center + Offset(0f, 4.dp.toPx())) }
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_play), contentDescription = null, tint = HomeNight.CyanLip, modifier = Modifier.size(26.dp))
            }
            Column(Modifier.weight(1f)) {
                BasicText(
                    stringResource(R.string.mode_vs_ai),
                    style = TextStyle(color = HomeNight.OnCyan, fontWeight = FontWeight.Black),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 18.sp, maxFontSize = 27.sp),
                )
                BasicText(
                    stringResource(R.string.mode_vs_ai_detail),
                    style = TextStyle(color = HomeNight.OnCyanSoft, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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

/** A frosted-glass mode row: coloured icon chip, title and detail, chevron. */
@Composable
internal fun FrostModeButton(
    @StringRes title: Int,
    @StringRes detail: Int,
    @DrawableRes icon: Int,
    chip: Color,
    chipLip: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RaisedButton(
        onClick = onClick,
        face = HomeNight.Frost,
        lip = HomeNight.FrostLip,
        shape = RoundedCornerShape(18.dp),
        lipHeight = 5.dp,
        modifier = modifier,
        faceModifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        border = HomeNight.FrostBorder,
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
                    style = TextStyle(color = HomeNight.OnFrostSoft, fontWeight = FontWeight.ExtraBold),
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
