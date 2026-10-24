package com.matinstudios.baghchal.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.ads.BannerAd
import com.matinstudios.baghchal.ads.BannerPlacement
import com.matinstudios.baghchal.data.SavedGame
import com.matinstudios.baghchal.engine.GOATS_TO_WIN
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.engine.ai.Difficulty
import com.matinstudios.baghchal.ui.theme.BaghchalTheme

@Composable
fun HomeScreen(
    onPlayVsAi: () -> Unit,
    onStartGame: (GameSetup) -> Unit,
    onResume: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    onHowToPlay: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val savedGame by viewModel.savedGame.collectAsStateWithLifecycle()
    val showBanner by viewModel.showBanner.collectAsStateWithLifecycle()
    Column(modifier.fillMaxSize()) {
        HomeContent(savedGame, onPlayVsAi, onStartGame, onResume, onStats, onSettings, onHowToPlay, Modifier.weight(1f))
        if (showBanner) BannerAd(BannerPlacement.HOME, Modifier.padding(top = 8.dp))
    }
}

/** Home: resume a saved game, or choose Player vs AI / Player vs Player. */
@Composable
fun HomeContent(
    savedGame: SavedGame?,
    onPlayVsAi: () -> Unit,
    onStartGame: (GameSetup) -> Unit,
    onResume: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    onHowToPlay: () -> Unit,
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
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(painterResource(R.drawable.ic_tiger), contentDescription = null, modifier = Modifier.size(64.dp))
            Image(painterResource(R.drawable.ic_goat), contentDescription = null, modifier = Modifier.size(64.dp))
        }
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
        )

        Column(
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (savedGame != null) ResumeCard(savedGame, onResume)

            Text(
                stringResource(R.string.mode_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            ModeCard(R.string.mode_vs_ai, R.string.mode_vs_ai_detail, onPlayVsAi)
            ModeCard(R.string.mode_vs_player, R.string.mode_vs_player_detail) { onStartGame(GameSetup.TWO_PLAYER) }

            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton(onClick = onHowToPlay) { Text(stringResource(R.string.home_how_to_play)) }
                TextButton(onClick = onStats) { Text(stringResource(R.string.home_stats)) }
                TextButton(onClick = onSettings) { Text(stringResource(R.string.home_settings)) }
            }
        }
    }
}

@Composable
private fun ModeCard(@StringRes title: Int, @StringRes detail: Int, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(stringResource(detail), style = MaterialTheme.typography.bodyMedium)
            }
            Text("›", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.clearAndSetSemantics {})
        }
    }
}

@Composable
private fun ResumeCard(savedGame: SavedGame, onResume: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.home_resume_title), style = MaterialTheme.typography.titleLarge)
            val aiSide = savedGame.aiSide
            val mode = if (aiSide == null) {
                stringResource(R.string.home_two_player_short)
            } else {
                val side = stringResource(if (aiSide.opponent == Side.GOAT) R.string.side_goats else R.string.side_tigers)
                stringResource(R.string.home_resume_vs_computer, side, stringResource(savedGame.difficulty.label))
            }
            Text(mode, style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(
                    R.string.home_resume_progress,
                    savedGame.game.goatsInHand,
                    savedGame.game.goatsCaptured,
                    GOATS_TO_WIN,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onResume, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_resume))
            }
        }
    }
}

internal val Difficulty.label: Int
    get() = when (this) {
        Difficulty.EASY -> R.string.difficulty_easy
        Difficulty.MEDIUM -> R.string.difficulty_medium
        Difficulty.HARD -> R.string.difficulty_hard
    }

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    BaghchalTheme { HomeContent(savedGame = null, onPlayVsAi = {}, onStartGame = {}, onResume = {}, onStats = {}, onSettings = {}, onHowToPlay = {}) }
}
