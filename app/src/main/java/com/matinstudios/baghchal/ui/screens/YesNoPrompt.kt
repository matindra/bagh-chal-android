package com.matinstudios.baghchal.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.matinstudios.baghchal.ui.board.rememberAnimationsOn
import com.matinstudios.baghchal.ui.theme.NightSkyMiddle
import com.matinstudios.baghchal.ui.theme.NightSkyTop
import kotlin.math.PI
import kotlin.math.sin

private val CardCorner = 28.dp
private val CardShape = RoundedCornerShape(CardCorner)
private val CardLipHeight = 6.dp
private val BadgeSize = 84.dp

/** Card and badge glass: Home's frost fading into the night sky. */
private val CardGlass = Brush.verticalGradient(listOf(HomeNight.Frost, NightSkyMiddle, NightSkyTop))

/** Places in Home's float/shine wave: card first, then the No button. */
private const val CARD_WAVE = 0
private const val NO_WAVE = 1

/**
 * The app's "are you sure?" prompt, in Home's night look: a frosted-glass card with a white rim
 * that rises in, floats like Home's buttons and shares their shine wave, with snow drifting inside.
 * "No" (the safe choice) is the coral button on the right; "Yes" is frosted glass.
 * Everything holds still when system animations are off.
 */
@Composable
fun YesNoPrompt(
    title: String,
    message: String,
    yes: String,
    no: String,
    @DrawableRes icon: Int,
    onYes: () -> Unit,
    onNo: () -> Unit,
) {
    FrostPrompt(
        title = title,
        message = message,
        onDismiss = onNo,
        badge = { Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(58.dp)) },
    ) { clock ->
        SecondaryButton(yes, onYes, Modifier.weight(1f))
        PrimaryButton(no, onNo, Modifier.weight(1f).buttonShine(clock, ButtonSize.Large))
    }
}

/**
 * A happy one-button note in the same frosted card, e.g. "Yay! New board unlocked".
 * [badge] fills the round badge on the card's top edge (clipped to a circle).
 */
@Composable
fun CelebratePrompt(
    title: String,
    message: String,
    ok: String,
    onOk: () -> Unit,
    badge: @Composable () -> Unit,
) {
    FrostPrompt(
        title = title,
        message = message,
        onDismiss = onOk,
        badge = { Box(Modifier.size(BadgeSize).clip(CircleShape), contentAlignment = Alignment.Center) { badge() } },
    ) { clock ->
        PrimaryButton(ok, onOk, Modifier.weight(1f).buttonShine(clock, ButtonSize.Large))
    }
}

@Composable
private fun FrostPrompt(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    badge: @Composable () -> Unit,
    buttons: @Composable RowScope.(State<Long>) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val animationsOn = rememberAnimationsOn()
        val clock = rememberSceneClock()
        val enter = remember { Animatable(if (animationsOn) 0f else 1f) }
        LaunchedEffect(Unit) {
            if (animationsOn) enter.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
        }

        Box(
            Modifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    val e = enter.value
                    alpha = e
                    translationY = (1f - e) * 32.dp.toPx()
                    scaleX = 0.94f + 0.06f * e
                    scaleY = 0.94f + 0.06f * e
                }
                .iceFloat(clock, CARD_WAVE),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier
                    .padding(top = BadgeSize / 2, bottom = CardLipHeight)
                    .fillMaxWidth()
                    .drawBehind {
                        drawRoundRect(
                            HomeNight.FrostLip,
                            topLeft = Offset(0f, CardLipHeight.toPx()),
                            size = size,
                            cornerRadius = CornerRadius(CardCorner.toPx()),
                        )
                    }
                    .background(CardGlass, CardShape)
                    .drawBehind {
                        clipToRounded(CardCorner) {
                            drawSnow(clock.value)
                            drawShineWave(clock, CARD_WAVE)
                        }
                    }
                    .border(1.5.dp, HomeNight.FrostBorder, CardShape)
                    .padding(start = 22.dp, end = 22.dp, top = BadgeSize / 2 + 14.dp, bottom = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    title,
                    color = HomeNight.OnFrost,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        shadow = Shadow(HomeNight.FrostLip, Offset(0f, 5f), blurRadius = 0f),
                    ),
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    message,
                    color = HomeNight.OnFrostSoft,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    buttons(clock)
                }
            }
            // The piece badge rides on the card's top edge: a frosted coin like Home's icon chips.
            Box(
                Modifier
                    .size(BadgeSize)
                    .drawBehind { drawCircle(HomeNight.FrostLip, center = center + Offset(0f, 5.dp.toPx())) }
                    .background(CardGlass, CircleShape)
                    .border(1.5.dp, HomeNight.FrostBorder, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                badge()
            }
        }
    }
}

/** Home's shine wave over a raised button's face (the part above its lip). */
private fun Modifier.buttonShine(clock: State<Long>, size: ButtonSize): Modifier = drawWithContent {
    drawContent()
    val faceHeight = this.size.height - size.lip.toPx()
    val path = Path().apply {
        addRoundRect(RoundRect(0f, 0f, this@drawWithContent.size.width, faceHeight, CornerRadius(size.corner.toPx())))
    }
    clipPath(path) { drawShineWave(clock, NO_WAVE) }
}

private inline fun DrawScope.clipToRounded(corner: Dp, block: DrawScope.() -> Unit) {
    val path = Path().apply {
        addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(corner.toPx())))
    }
    clipPath(path, block = block)
}

/** A snowflake inside the card: x as a fraction of the width, size in dp, fall time in ms. */
private class CardFlake(val x: Float, val radius: Float, val delay: Int, val duration: Int, val alpha: Float)

private val CardFlakes = listOf(
    CardFlake(0.08f, 2.5f, 0, 7000, 0.7f),
    CardFlake(0.24f, 1.5f, 2600, 8200, 0.5f),
    CardFlake(0.41f, 2f, 5200, 7600, 0.6f),
    CardFlake(0.58f, 1.5f, 1200, 9000, 0.5f),
    CardFlake(0.74f, 2.5f, 4100, 7300, 0.7f),
    CardFlake(0.9f, 2f, 6400, 8600, 0.6f),
)

/** Soft snow drifting down behind the text, like the falling snow on Home. */
private fun DrawScope.drawSnow(time: Long) {
    for (flake in CardFlakes) {
        val t = ((time + flake.delay) % flake.duration).toFloat() / flake.duration
        val r = flake.radius.dp.toPx()
        val sway = sin(t * 3f * PI.toFloat()) * 6.dp.toPx()
        val y = -r + t * (size.height + 2 * r)
        drawCircle(Color.White.copy(alpha = flake.alpha * 0.6f), r, Offset(size.width * flake.x + sway, y))
    }
}
