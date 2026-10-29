package com.matinstudios.baghchal.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.matinstudios.baghchal.R

/** Height of the mountain strip; screens leave about this much room at the end of their content. */
val MountainsHeight = 120.dp

/**
 * The night sky every screen sits on (Home draws its own animated version): navy gradient, two soft
 * aurora glows and, with [mountains], snowy peaks along the bottom. Decorative only: nothing here
 * is clickable, and the game screen goes without the mountains so nothing sits near the board or ad.
 */
@Composable
fun NightBackdrop(modifier: Modifier = Modifier, mountains: Boolean = true) {
    Canvas(modifier) {
        val unit = size.width / 390f
        drawRect(Brush.verticalGradient(0f to NightSkyTop, 0.5f to NightSkyMiddle, 1f to NightSkyBottom))
        drawGlow(Offset(110 * unit, 85.dp.toPx()), 150 * unit, 45.dp.toPx(), NightAuroraGreen)
        drawGlow(Offset(300 * unit, 140.dp.toPx()), 150 * unit, 40.dp.toPx(), NightAuroraViolet)
        if (mountains) drawNightMountains(top = size.height - MountainsHeight.toPx(), unit = unit)
    }
}

private fun DrawScope.drawGlow(center: Offset, rx: Float, ry: Float, color: Color) {
    withTransform({ scale(1f, ry / rx, pivot = center) }) {
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.4f), Color.Transparent), center, rx), rx, center)
    }
}

/**
 * Snowy peaks from [top] to the bottom of the canvas: x in design units ([unit] px each, 390 wide),
 * heights in dp. Shared with Home's animated sky.
 */
fun DrawScope.drawNightMountains(top: Float, unit: Float) {
    val v = 1.dp.toPx()
    fun pt(x: Float, y: Float) = Offset(x * unit, top + y * v)
    fun poly(color: Color, vararg points: Pair<Float, Float>) {
        val path = Path()
        points.forEachIndexed { i, (x, y) -> pt(x, y).let { if (i == 0) path.moveTo(it.x, it.y) else path.lineTo(it.x, it.y) } }
        path.close()
        drawPath(path, color)
    }
    val bottom = (size.height - top) / v
    poly(
        NightMountain,
        0f to 90f, 50f to 40f, 95f to 80f, 160f to 10f, 225f to 85f, 290f to 25f, 350f to 75f, 390f to 50f,
        390f to bottom, 0f to bottom,
    )
    poly(NightSnowCap, 160f to 10f, 146f to 26f, 156f to 24f, 163f to 30f, 170f to 23f, 178f to 27f)
    poly(NightSnowCap, 290f to 25f, 278f to 39f, 286f to 37f, 292f to 43f, 298f to 36f, 304f to 39f)
    poly(NightSnowCap, 50f to 40f, 40f to 50f, 47f to 49f, 52f to 53f, 58f to 48f)
}

/** A string of Nepali prayer flags, used as a header ornament. */
@Composable
fun PrayerFlags(modifier: Modifier = Modifier) {
    Image(
        painterResource(R.drawable.deco_prayer_flags),
        contentDescription = null,
        contentScale = ContentScale.FillWidth,
        modifier = modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .alpha(BaghchalThemeExtras.colors.flagsAlpha),
    )
}
