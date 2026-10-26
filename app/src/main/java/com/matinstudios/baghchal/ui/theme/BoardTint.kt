package com.matinstudios.baghchal.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.matinstudios.baghchal.data.BoardTheme

/**
 * A board's colours, carried into the rest of the app in a lighter shade (darker in dark mode):
 * [hue]/[saturation] wash the neutral colours (backgrounds, cards, outlines, body text) and
 * [accentHue]/[accentSaturation] replace the sky-blue third colour (online cards, links).
 * Tiger orange and goat green never change: they say which side is which.
 */
data class BoardTint(
    val hue: Float,
    val saturation: Float,
    val accentHue: Float,
    val accentSaturation: Float,
)

/** Null for Wood: its warm parchment and sky palette is the app's own. */
val BoardTheme.tint: BoardTint?
    get() = when (this) {
        BoardTheme.WOOD -> null
        BoardTheme.STONE -> BoardTint(hue = 210f, saturation = 0.12f, accentHue = 205f, accentSaturation = 0.22f)
        BoardTheme.CLOTH -> BoardTint(hue = 226f, saturation = 0.50f, accentHue = 226f, accentSaturation = 0.45f)
        BoardTheme.NIGHT -> BoardTint(hue = 230f, saturation = 0.55f, accentHue = 232f, accentSaturation = 0.50f)
        BoardTheme.THANGKA -> BoardTint(hue = 4f, saturation = 0.60f, accentHue = 356f, accentSaturation = 0.60f)
        BoardTheme.JUNGLE -> BoardTint(hue = 85f, saturation = 0.45f, accentHue = 65f, accentSaturation = 0.55f)
        BoardTheme.MANDALA -> BoardTint(hue = 36f, saturation = 0.80f, accentHue = 176f, accentSaturation = 0.60f)
        BoardTheme.DHAKA -> BoardTint(hue = 355f, saturation = 0.55f, accentHue = 352f, accentSaturation = 0.65f)
        BoardTheme.BRASS -> BoardTint(hue = 42f, saturation = 0.70f, accentHue = 44f, accentSaturation = 0.75f)
        BoardTheme.INK -> BoardTint(hue = 40f, saturation = 0.22f, accentHue = 30f, accentSaturation = 0.08f)
        BoardTheme.SNOW -> BoardTint(hue = 207f, saturation = 0.55f, accentHue = 207f, accentSaturation = 0.55f)
    }

/** This scheme with its neutral and third (tertiary) colours redrawn in [tint]. */
fun ColorScheme.tinted(tint: BoardTint, dark: Boolean): ColorScheme {
    fun c(lightness: Float, saturationScale: Float = 1f) =
        Color.hsl(tint.hue, (tint.saturation * saturationScale).coerceIn(0f, 1f), lightness)
    fun a(lightness: Float) = Color.hsl(tint.accentHue, tint.accentSaturation, lightness)
    return if (dark) {
        copy(
            tertiary = a(0.78f),
            onTertiary = a(0.18f),
            tertiaryContainer = a(0.27f),
            onTertiaryContainer = a(0.90f),
            background = c(0.085f, 0.50f),
            onBackground = c(0.91f, 0.35f),
            surface = c(0.085f, 0.50f),
            onSurface = c(0.91f, 0.35f),
            surfaceVariant = c(0.28f, 0.35f),
            onSurfaceVariant = c(0.80f, 0.30f),
            outline = c(0.58f, 0.22f),
            outlineVariant = c(0.28f, 0.35f),
            surfaceBright = c(0.25f, 0.45f),
            surfaceDim = c(0.085f, 0.50f),
            surfaceContainerLowest = c(0.06f, 0.50f),
            surfaceContainerLow = c(0.115f, 0.45f),
            surfaceContainer = c(0.135f, 0.45f),
            surfaceContainerHigh = c(0.175f, 0.40f),
            surfaceContainerHighest = c(0.215f, 0.40f),
        )
    } else {
        copy(
            tertiary = a(0.30f),
            onTertiary = Color.White,
            tertiaryContainer = a(0.87f),
            onTertiaryContainer = a(0.12f),
            background = c(0.955f),
            onBackground = c(0.11f, 0.45f),
            surface = c(0.955f),
            onSurface = c(0.11f, 0.45f),
            surfaceVariant = c(0.86f, 0.70f),
            onSurfaceVariant = c(0.28f, 0.30f),
            outline = c(0.50f, 0.22f),
            outlineVariant = c(0.78f, 0.40f),
            surfaceBright = c(0.955f),
            surfaceDim = c(0.84f, 0.60f),
            surfaceContainerLowest = c(0.985f),
            surfaceContainerLow = c(0.94f, 0.90f),
            surfaceContainer = c(0.92f, 0.85f),
            surfaceContainerHigh = c(0.90f, 0.80f),
            surfaceContainerHighest = c(0.88f, 0.75f),
        )
    }
}
