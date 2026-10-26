package com.matinstudios.baghchal.ui.screens

import androidx.annotation.DrawableRes
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.data.Record
import com.matinstudios.baghchal.data.Stats
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.engine.ai.Difficulty
import com.matinstudios.baghchal.ui.board.LocalPieceArt
import com.matinstudios.baghchal.ui.theme.BaghchalTheme

@Composable
fun StatsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = viewModel(factory = StatsViewModel.Factory),
) {
    val stats = viewModel.stats.collectAsStateWithLifecycle().value ?: return
    StatsContent(stats, onBack = onBack, onReset = viewModel::onReset, modifier = modifier)
}

@Composable
fun StatsContent(stats: Stats, onBack: () -> Unit, onReset: () -> Unit, modifier: Modifier = Modifier) {
    var confirmReset by rememberSaveable { mutableStateOf(false) }

    Column(modifier.fillMaxSize()) {
        BackTopBar(onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.stats_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.stats_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Column(Modifier.widthIn(max = 480.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SideCard(stats, Side.GOAT, LocalPieceArt.current.goat, R.string.stats_as_goats)
                SideCard(stats, Side.TIGER, LocalPieceArt.current.tiger, R.string.stats_as_tigers)
            }
            TextButton(onClick = { confirmReset = true }) { Text(stringResource(R.string.stats_reset)) }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.stats_reset_title)) },
            text = { Text(stringResource(R.string.stats_reset_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    onReset()
                }) { Text(stringResource(R.string.stats_reset)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun SideCard(stats: Stats, side: Side, @DrawableRes icon: Int, @StringRes title: Int) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(32.dp))
                Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
            }
            StatsRow(
                label = "",
                values = listOf(
                    stringResource(R.string.stats_wins),
                    stringResource(R.string.stats_losses),
                    stringResource(R.string.stats_draws),
                ),
                header = true,
            )
            for (difficulty in Difficulty.entries) {
                StatsRow(stringResource(difficulty.label), stats.record(side, difficulty).values())
            }
            HorizontalDivider()
            StatsRow(stringResource(R.string.stats_total), stats.total(side).values(), header = true)
        }
    }
}

private fun Record.values() = listOf(wins, losses, draws).map { it.toString() }

@Composable
private fun StatsRow(label: String, values: List<String>, header: Boolean = false) {
    val style = if (header) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyLarge
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = style, modifier = Modifier.weight(1.5f))
        for (value in values) {
            Text(value, style = style, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StatsContentPreview() {
    BaghchalTheme { StatsContent(Stats(), onBack = {}, onReset = {}) }
}
