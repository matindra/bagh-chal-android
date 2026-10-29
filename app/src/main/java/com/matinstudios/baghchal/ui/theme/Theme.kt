package com.matinstudios.baghchal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The night palette: navy sky behind everything, frosted-glass containers (white text), and
 * glass tints of coral (tigers), green (goats) and cyan for the containers.
 */
private val NightColorScheme = darkColorScheme(
    primary = Color(0xFFFF9A6B),
    onPrimary = Color(0xFF4A1400),
    primaryContainer = Color(0xFF6C4059), // coral glass over the mountains
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = Color(0xFF7FE3A8),
    onSecondary = Color(0xFF003920),
    secondaryContainer = Color(0xFF26646A), // green glass
    onSecondaryContainer = Color(0xFFC8F5D6),
    tertiary = Color(0xFF7FDDF7),
    onTertiary = Color(0xFF00344A),
    tertiaryContainer = Color(0xFF306B9F), // cyan glass
    onTertiaryContainer = Color(0xFFD2F1FF),
    background = NightSkyMiddle,
    onBackground = Color.White,
    surface = NightSkyMiddle,
    onSurface = Color.White,
    surfaceVariant = NightFrost,
    onSurfaceVariant = NightOnFrostSoft,
    surfaceTint = Color.Transparent,
    inverseSurface = NightSnowCap,
    inverseOnSurface = NightSkyMiddle,
    inversePrimary = NightCoralLip,
    outline = Color(0xFF8E9CCB),
    outlineVariant = Color(0xFF4A5D96),
    surfaceBright = Color(0xFF4A5E9A),
    surfaceDim = Color(0xFF0F1E48),
    surfaceContainerLowest = Color(0xFF13234F),
    surfaceContainerLow = Color(0xFF22366E),
    surfaceContainer = Color(0xFF2C4079),
    surfaceContainerHigh = Color(0xFF34488A),
    surfaceContainerHighest = NightFrost,
)

/** Game colours outside Material's roles. */
@Immutable
data class GameColors(
    val easy: Color,
    val medium: Color,
    val hard: Color,
    val hintContainer: Color,
    val onHintContainer: Color,
    val friend: Color,
    val onFriend: Color,
    val friendContainer: Color,
    val onFriendContainer: Color,
    val mountainsAlpha: Float,
    val flagsAlpha: Float,
)

private val NightGameColors = GameColors(
    EasyDark, MediumDark, HardDark,
    hintContainer = HintContainerDark,
    onHintContainer = OnHintContainerDark,
    friend = RhododendronDark,
    onFriend = OnRhododendronDark,
    friendContainer = RhododendronContainerDark,
    onFriendContainer = OnRhododendronContainerDark,
    mountainsAlpha = 1f, flagsAlpha = 0.9f,
)

private val LocalGameColors = staticCompositionLocalOf { NightGameColors }

object BaghchalThemeExtras {
    val colors: GameColors
        @Composable @ReadOnlyComposable get() = LocalGameColors.current
}

/**
 * The app's one look: the night palette on every screen, like Home, whatever the phone's light/dark
 * setting or the chosen board. Dynamic (wallpaper) colour is deliberately off.
 */
@Composable
fun BaghchalTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalGameColors provides NightGameColors) {
        MaterialTheme(
            colorScheme = NightColorScheme,
            typography = Typography,
            content = content,
        )
    }
}
