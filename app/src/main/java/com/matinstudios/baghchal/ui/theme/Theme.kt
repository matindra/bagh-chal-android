package com.matinstudios.baghchal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.matinstudios.baghchal.data.BoardTheme

private val LightColorScheme = lightColorScheme(
    primary = TigerOrange40,
    onPrimary = Color.White,
    primaryContainer = TigerOrange90,
    onPrimaryContainer = TigerOrange10,
    secondary = Meadow40,
    onSecondary = Color.White,
    secondaryContainer = Meadow90,
    onSecondaryContainer = Meadow10,
    tertiary = Sky40,
    onTertiary = Color.White,
    tertiaryContainer = Sky90,
    onTertiaryContainer = Sky10,
    background = Parchment,
    onBackground = Ink,
    surface = Parchment,
    onSurface = Ink,
    surfaceVariant = ParchmentVariant,
    onSurfaceVariant = InkVariant,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    surfaceBright = Parchment,
    surfaceDim = SurfaceDimLight,
    surfaceContainerLowest = ContainerLowestLight,
    surfaceContainerLow = ContainerLowLight,
    surfaceContainer = ContainerLight,
    surfaceContainerHigh = ContainerHighLight,
    surfaceContainerHighest = ContainerHighestLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = TigerOrange80,
    onPrimary = TigerOrange20,
    primaryContainer = TigerOrange30,
    onPrimaryContainer = TigerOrange90,
    secondary = Meadow80,
    onSecondary = Meadow20,
    secondaryContainer = Meadow30,
    onSecondaryContainer = Meadow90,
    tertiary = Sky80,
    onTertiary = Sky20,
    tertiaryContainer = Sky30,
    onTertiaryContainer = Sky90,
    background = NightSoil,
    onBackground = Moonlight,
    surface = NightSoil,
    onSurface = Moonlight,
    surfaceVariant = InkVariant,
    onSurfaceVariant = OutlineVariantLight,
    outline = OutlineDark,
    outlineVariant = InkVariant,
    surfaceBright = SurfaceBrightDark,
    surfaceDim = NightSoil,
    surfaceContainerLowest = ContainerLowestDark,
    surfaceContainerLow = ContainerLowDark,
    surfaceContainer = ContainerDark,
    surfaceContainerHigh = ContainerHighDark,
    surfaceContainerHighest = ContainerHighestDark,
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

private val LightGameColors = GameColors(
    EasyLight, MediumLight, HardLight,
    hintContainer = HintContainerLight,
    onHintContainer = OnHintContainerLight,
    friend = RhododendronLight,
    onFriend = Color.White,
    friendContainer = RhododendronContainerLight,
    onFriendContainer = OnRhododendronContainerLight,
    mountainsAlpha = 0.6f, flagsAlpha = 1f,
)
private val DarkGameColors = GameColors(
    EasyDark, MediumDark, HardDark,
    hintContainer = HintContainerDark,
    onHintContainer = OnHintContainerDark,
    friend = RhododendronDark,
    onFriend = OnRhododendronDark,
    friendContainer = RhododendronContainerDark,
    onFriendContainer = OnRhododendronContainerDark,
    mountainsAlpha = 0.15f, flagsAlpha = 0.75f,
)

private val LocalGameColors = staticCompositionLocalOf { LightGameColors }

object BaghchalThemeExtras {
    val colors: GameColors
        @Composable @ReadOnlyComposable get() = LocalGameColors.current
}

/**
 * The app's own palette in light and dark. Dynamic (wallpaper) colour is deliberately off:
 * it washed the warm artwork out to greys. The neutral colours follow the chosen [boardTheme].
 */
@Composable
fun BaghchalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    boardTheme: BoardTheme = BoardTheme.WOOD,
    content: @Composable () -> Unit,
) {
    val base = if (darkTheme) DarkColorScheme else LightColorScheme
    val colorScheme = boardTheme.tint?.let { base.tinted(it, darkTheme) } ?: base
    CompositionLocalProvider(LocalGameColors provides if (darkTheme) DarkGameColors else LightGameColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
