package com.matinstudios.baghchal.ui.screens

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.data.SavedGame
import com.matinstudios.baghchal.engine.GOATS_TO_WIN
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.engine.ai.Difficulty
import com.matinstudios.baghchal.ui.theme.BaghchalTheme

@Composable
fun HomeScreen(
    onStartGame: (GameSetup) -> Unit,
    onResume: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val savedGame by viewModel.savedGame.collectAsStateWithLifecycle()
    HomeContent(savedGame, onStartGame, onResume, onStats, onSettings, modifier)
}

@Composable
fun HomeContent(
    savedGame: SavedGame?,
    onStartGame: (GameSetup) -> Unit,
    onResume: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var humanSide by rememberSaveable { mutableStateOf(Side.GOAT) }
    var difficulty by rememberSaveable { mutableStateOf(Difficulty.MEDIUM) }

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

            Text(stringResource(R.string.home_vs_computer), style = MaterialTheme.typography.titleLarge)

            Text(stringResource(R.string.home_play_as), style = MaterialTheme.typography.labelLarge)
            val sides = listOf(Side.GOAT to R.string.side_goats, Side.TIGER to R.string.side_tigers)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                sides.forEachIndexed { index, (side, label) ->
                    SegmentedButton(
                        selected = humanSide == side,
                        onClick = { humanSide = side },
                        shape = SegmentedButtonDefaults.itemShape(index, sides.size),
                    ) { Text(stringResource(label)) }
                }
            }

            Text(stringResource(R.string.home_difficulty), style = MaterialTheme.typography.labelLarge)
            val levels = Difficulty.entries
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                levels.forEachIndexed { index, level ->
                    SegmentedButton(
                        selected = difficulty == level,
                        onClick = { difficulty = level },
                        shape = SegmentedButtonDefaults.itemShape(index, levels.size),
                    ) { Text(stringResource(level.label)) }
                }
            }

            Button(
                onClick = { onStartGame(GameSetup.vsComputer(humanSide, difficulty)) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.home_play)) }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            OutlinedButton(
                onClick = { onStartGame(GameSetup.TWO_PLAYER) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.home_two_player)) }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton(onClick = onStats) { Text(stringResource(R.string.home_stats)) }
                TextButton(onClick = onSettings) { Text(stringResource(R.string.home_settings)) }
            }
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
    BaghchalTheme { HomeContent(savedGame = null, onStartGame = {}, onResume = {}, onStats = {}, onSettings = {}) }
}
