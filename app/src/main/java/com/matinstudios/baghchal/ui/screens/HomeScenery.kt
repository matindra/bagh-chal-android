package com.matinstudios.baghchal.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.withFrameMillis
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.ui.board.ChaseSceneryColors
import com.matinstudios.baghchal.ui.board.rememberAnimationsOn
import com.matinstudios.baghchal.ui.theme.NightAuroraGreen
import com.matinstudios.baghchal.ui.theme.NightAuroraViolet
import com.matinstudios.baghchal.ui.theme.NightCoral
import com.matinstudios.baghchal.ui.theme.NightCoralLip
import com.matinstudios.baghchal.ui.theme.NightFrost
import com.matinstudios.baghchal.ui.theme.NightMountain
import com.matinstudios.baghchal.ui.theme.NightOnFrostSoft
import com.matinstudios.baghchal.ui.theme.NightSkyBottom
import com.matinstudios.baghchal.ui.theme.NightSkyMiddle
import com.matinstudios.baghchal.ui.theme.NightSkyTop
import com.matinstudios.baghchal.ui.theme.NightSnowCap
import com.matinstudios.baghchal.ui.theme.drawNightMountains
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Home's "Himalaya snow night" look. Fixed colours: the scene is always night, whatever the app
 * theme or board, so text colours here are chosen against these backgrounds.
 */
internal object HomeNight {
    val SkyTop = NightSkyTop
    val SkyMiddle = NightSkyMiddle
    val SkyBottom = NightSkyBottom
    val Aurora1 = NightAuroraGreen
    val Aurora2 = NightAuroraViolet
    val Mountain = NightMountain
    val SnowCap = NightSnowCap

    /** Opaque "frosted glass" face: white at ~12% over the sky. */
    val Frost = NightFrost
    val FrostBorder = Color.White.copy(alpha = 0.28f)
    val FrostLip = Color.Black.copy(alpha = 0.35f)
    val OnFrost = Color.White
    val OnFrostSoft = NightOnFrostSoft
    val Link = Color(0xFFFFE08A)

    val Gold = Color(0xFFFFC93C)
    val GoldLip = Color(0xFFB38B12)
    val OnGold = Color(0xFF3A2600)

    val Cyan = Color(0xFF4FD1F5)
    val CyanLip = Color(0xFF1E8FB3)
    val OnCyan = Color(0xFF06324A)

    val Violet = Color(0xFF9B7BFF)
    val VioletLip = Color(0xFF5E43C2)
    val OnViolet = Color.White

    val Coral = NightCoral
    val CoralLip = NightCoralLip
    val CoralInk = Color(0xFF9A2E14)

    val IceTop = Color(0xFFF4FBFF)
    val IceBottom = Color(0xFFD7ECFF)
    val IceShade = Color(0xFFA9CBEB)
    val IceLip = Color(0xFF4E78A8)
    val IceInk = Color(0xFF1C2F66)
    val IceInkSoft = Color(0xFF2E4A8C)
    val IceStar = Color(0xFF7FB2E8)
    val Chain = Color(0xFFC9D6FF)

    val FrameFace = Color(0xFFDCEBFF)

    val ChaseScenery = ChaseSceneryColors(
        skyTop = Color(0xFFC9D8FF),
        skyBottom = Color(0xFFF2F5FF),
        farHill = Color(0xFFA8B8E8),
        nearHill = Color.White,
    )
}

/**
 * Milliseconds since Home appeared, ticking every frame; stays 0 when system animations are off
 * or while [running] is false (e.g. a closed side menu).
 */
@Composable
internal fun rememberSceneClock(running: Boolean = true): State<Long> {
    val animationsOn = rememberAnimationsOn()
    return produceState(0L, animationsOn, running) {
        if (!animationsOn || !running) return@produceState
        val start = withFrameMillis { it }
        while (true) withFrameMillis { value = it - start }
    }
}

private fun wave(t: Long, periodMillis: Int, phase: Float = 0f): Float =
    sin(2 * PI * (t.toDouble() / periodMillis + phase)).toFloat()

/** A drifting snowflake: x is in design units (390 wide), times in ms. */
private class Flake(val x: Float, val size: Float, val delay: Int, val duration: Int, val alpha: Float = 1f)

private val Flakes = listOf(
    Flake(20f, 5f, 0, 9000),
    Flake(60f, 3f, 3000, 11000),
    Flake(104f, 4f, 6000, 9000),
    Flake(150f, 6f, 1500, 12000, 0.8f),
    Flake(196f, 3f, 7500, 9000),
    Flake(240f, 5f, 4200, 10000),
    Flake(286f, 4f, 8400, 9000),
    Flake(330f, 6f, 2600, 11500, 0.8f),
    Flake(370f, 3f, 5300, 9000),
)

/**
 * Night sky behind Home: gradient, two drifting aurora glows, snowy peaks and falling snow.
 * Purely decorative and drawn only inside its own bounds, so it never reaches the banner.
 */
@Composable
internal fun NightSkyBackdrop(clock: State<Long>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val t = clock.value
        val unit = size.width / 390f // design width → px
        drawRect(Brush.verticalGradient(0f to HomeNight.SkyTop, 0.5f to HomeNight.SkyMiddle, 1f to HomeNight.SkyBottom))
        drawAurora(Offset(110 * unit, 85.dp.toPx()), 150 * unit, 45.dp.toPx(), HomeNight.Aurora1, t, 0f)
        drawAurora(Offset(300 * unit, 140.dp.toPx()), 150 * unit, 40.dp.toPx(), HomeNight.Aurora2, t, 0.5f)
        drawNightMountains(top = 300.dp.toPx(), unit = unit)
        for (flake in Flakes) drawFlake(flake, t, unit)
    }
}

private fun DrawScope.drawAurora(center: Offset, rx: Float, ry: Float, color: Color, t: Long, phase: Float) {
    // Eases side to side and swells, like the CSS `aurora` keyframes (10 s).
    val c = -cos(2 * PI * (t / 10_000.0 + phase)).toFloat() // -1 → 1 → -1
    val swell = (c + 1f) / 2f
    val alpha = 0.35f + 0.25f * swell
    val at = center + Offset(30.dp.toPx() * c, 0f)
    withTransform({ scale(1f, ry * (1f + 0.25f * swell) / rx, pivot = at) }) {
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), at, rx), rx, at)
    }
}

private fun DrawScope.drawFlake(flake: Flake, t: Long, unit: Float) {
    val p = ((t + flake.delay) % flake.duration).toFloat() / flake.duration
    // Falls the full height, swaying right then back left on the way down.
    val dx = if (p < 0.5f) 14f * (p / 0.5f) else 14f - 20f * ((p - 0.5f) / 0.5f)
    val y = -20.dp.toPx() + p * (size.height + 40.dp.toPx())
    val r = flake.size * unit / 2
    drawCircle(Color.White.copy(alpha = flake.alpha), r, Offset((flake.x + dx) * unit + r, y))
}

/** Four-pointed twinkle; [t] 0..1 through its cycle. */
private fun DrawScope.drawTwinkle(center: Offset, size: Float, color: Color, t: Float) {
    val s = (1f - cos(2 * PI * t).toFloat()) / 2f // 0 → 1 → 0
    val r = size / 2 * (0.4f + 0.6f * s)
    val w = r * 0.18f
    val path = Path().apply {
        moveTo(center.x, center.y - r)
        lineTo(center.x + w, center.y - w)
        lineTo(center.x + r, center.y)
        lineTo(center.x + w, center.y + w)
        lineTo(center.x, center.y + r)
        lineTo(center.x - w, center.y + w)
        lineTo(center.x - r, center.y)
        lineTo(center.x - w, center.y - w)
        close()
    }
    withTransform({ rotate(45f * s, center) }) { drawPath(path, color.copy(alpha = 0.15f + 0.85f * s)) }
}

private val SignWidth = 280.dp
private val ChainLength = 76.dp
private val PlaqueTop = 72.dp
private val PlaqueHeight = 80.dp
private val PlaqueLip = 7.dp

/** Height Home's header takes: the chains, the plaque and its lip. */
internal val IceSignHeight = PlaqueTop + PlaqueHeight + PlaqueLip

/** The game's name on an ice plaque hanging from two chains, swinging gently from the top. */
@Composable
internal fun IceSign(clock: State<Long>, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    Box(
        modifier
            .width(SignWidth)
            .height(IceSignHeight)
            .graphicsLayer {
                transformOrigin = TransformOrigin(0.5f, 0f)
                rotationZ = 2f * wave(clock.value, 4400)
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val dot = 4.dp.toPx()
            for (x in listOf(49.dp.toPx(), 231.dp.toPx())) {
                var y = dot / 2
                while (y < ChainLength.toPx()) {
                    drawCircle(HomeNight.Chain, dot / 2, Offset(x, y))
                    y += dot * 2
                }
            }
        }
        Column(
            Modifier
                .offset(y = PlaqueTop)
                .width(SignWidth)
                .height(PlaqueHeight)
                .drawBehind { drawPlaque(clock.value) }
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BasicText(
                stringResource(R.string.home_name).uppercase(),
                style = TextStyle(
                    color = HomeNight.IceInk,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    shadow = Shadow(HomeNight.IceShade, with(density) { Offset(0f, 3.dp.toPx()) }, 0f),
                ),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 20.sp, maxFontSize = 40.sp),
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.home_tagline),
                color = HomeNight.IceInkSoft,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp,
                maxLines = 1,
            )
        }
    }
}

private fun DrawScope.drawPlaque(t: Long) {
    val radius = CornerRadius(20.dp.toPx())
    val lip = PlaqueLip.toPx()
    val face = Size(size.width, size.height)
    drawRoundRect(HomeNight.IceLip, Offset(0f, lip), face, radius)
    val shape = Path().apply { addRoundRect(RoundRect(0f, 0f, face.width, face.height, radius)) }
    clipPath(shape) {
        drawRect(Brush.verticalGradient(listOf(HomeNight.IceTop, HomeNight.IceBottom)))
        drawRect(Color.White, size = Size(face.width, 4.dp.toPx()))
        drawRect(HomeNight.IceShade, Offset(0f, face.height - 6.dp.toPx()), Size(face.width, 6.dp.toPx()))
    }
    drawTwinkle(Offset(18.dp.toPx(), 16.dp.toPx()), 16.dp.toPx(), HomeNight.IceStar, (t % 2400) / 2400f)
    drawTwinkle(
        Offset(face.width - 19.dp.toPx(), face.height - 19.dp.toPx()),
        14.dp.toPx(),
        HomeNight.IceStar,
        ((t + 1200) % 2400) / 2400f,
    )
}

/** Frosty frame around the chase scene, with a darker lip underneath. */
@Composable
internal fun FrostFrame(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val outer = RoundedCornerShape(25.dp)
    Box(
        modifier
            .padding(bottom = 7.dp)
            .drawBehind {
                drawRoundRect(HomeNight.IceLip, Offset(0f, 7.dp.toPx()), size, CornerRadius(25.dp.toPx()))
            }
            .background(HomeNight.FrameFace, outer)
            .padding(5.dp),
    ) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)), content = content)
    }
}
