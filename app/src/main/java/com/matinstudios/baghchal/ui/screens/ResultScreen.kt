package com.matinstudios.baghchal.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.engine.ai.Difficulty
import com.matinstudios.baghchal.ui.theme.BaghchalTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Artwork aspect ratio of `result_bg_*` (300×200). */
private const val ART_ASPECT_RATIO = 1.5f
/** Lets the screen's fade-in transition mostly finish before the artwork animates. */
private const val ART_START_DELAY_MILLIS = 350L
private const val GOAT_ENTRY_MILLIS = 450
private const val GOAT_ENTRY_STAGGER_MILLIS = 180L

@Composable
fun ResultScreen(
    result: GameResult,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        Box(
            Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .aspectRatio(ART_ASPECT_RATIO)
                .clip(MaterialTheme.shapes.extraLarge),
        ) {
            when (result.winner) {
                Side.TIGER -> TigersWinArt()
                Side.GOAT -> GoatsWinArt()
            }
        }
        Text(
            stringResource(if (result.winner == Side.TIGER) R.string.result_title_tigers else R.string.result_title_goats),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            pluralStringResource(R.plurals.result_moves, result.moveCount, result.moveCount),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onHome) { Text(stringResource(R.string.home)) }
            Button(onClick = onPlayAgain) { Text(stringResource(R.string.play_again)) }
        }
    }
}

/** Large tiger popping in over the sunburst. */
@Composable
private fun TigersWinArt() {
    val description = stringResource(R.string.cd_result_tigers)
    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(ART_START_DELAY_MILLIS)
        launch { alpha.animateTo(1f, tween(250)) }
        scale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
    }
    BoxWithConstraints(Modifier.fillMaxSize().semantics { contentDescription = description }) {
        Background(R.drawable.result_bg_tiger)
        Image(
            painterResource(R.drawable.ic_tiger),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .size(maxHeight * 0.72f)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    this.alpha = alpha.value
                },
        )
    }
}

/** Small locked-in tiger at the back, three goats rising in front of it one after another. */
@Composable
private fun GoatsWinArt() {
    val description = stringResource(R.string.cd_result_goats)
    val goats = remember { List(3) { Animatable(0f) } }
    LaunchedEffect(Unit) {
        goats.forEachIndexed { index, progress ->
            launch {
                delay(ART_START_DELAY_MILLIS + index * GOAT_ENTRY_STAGGER_MILLIS)
                progress.animateTo(1f, tween(GOAT_ENTRY_MILLIS, easing = FastOutSlowInEasing))
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().semantics { contentDescription = description }) {
        Background(R.drawable.result_bg_goat)
        val tigerSize = maxHeight * 0.42f
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = maxHeight * 0.1f)
                .size(tigerSize),
        ) {
            Image(painterResource(R.drawable.ic_tiger), contentDescription = null, modifier = Modifier.fillMaxSize())
            Image(
                painterResource(R.drawable.lock_badge),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = tigerSize * 0.12f, y = -tigerSize * 0.08f)
                    .size(tigerSize * 0.42f),
            )
        }
        val goatSize = maxHeight * 0.44f
        // Left, right, then the middle goat in front.
        for ((index, x) in listOf(-0.24f, 0.24f, 0f).withIndex()) {
            val progress = goats[index].value
            Goat(
                size = goatSize,
                x = maxWidth * x,
                riseFrom = maxHeight,
                progress = progress,
            )
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.Goat(size: Dp, x: Dp, riseFrom: Dp, progress: Float) {
    Image(
        painterResource(R.drawable.ic_goat),
        contentDescription = null,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .offset(x = x, y = -maxHeight * 0.04f + riseFrom * (1f - progress))
            .size(size)
            .graphicsLayer { alpha = progress },
    )
}

@Composable
private fun Background(@DrawableRes art: Int) {
    Image(
        painterResource(art),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier.fillMaxSize(),
    )
}

@Preview(showBackground = true)
@Composable
private fun TigersWinPreview() {
    BaghchalTheme {
        ResultScreen(GameResult(Side.TIGER, 41, GameSetup.TWO_PLAYER), onPlayAgain = {}, onHome = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun GoatsWinPreview() {
    BaghchalTheme {
        ResultScreen(
            GameResult(Side.GOAT, 87, GameSetup.vsComputer(Side.GOAT, Difficulty.HARD)),
            onPlayAgain = {},
            onHome = {},
        )
    }
}
