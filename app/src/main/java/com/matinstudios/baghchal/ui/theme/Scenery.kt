package com.matinstudios.baghchal.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.matinstudios.baghchal.R

/** Height of the mountain strip; screens leave about this much room at the end of their content. */
val MountainsHeight = 120.dp

/**
 * A soft sky gradient with the Himalaya along the bottom, drawn behind [content].
 * Decorative only: nothing here is clickable, and screens keep ads outside it.
 */
@Composable
fun MountainBackdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier.background(Brush.verticalGradient(listOf(colors.surface, colors.surfaceContainerLow))),
    ) {
        Image(
            painterResource(R.drawable.deco_mountains),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(MountainsHeight)
                .alpha(BaghchalThemeExtras.colors.mountainsAlpha),
        )
        content()
    }
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
