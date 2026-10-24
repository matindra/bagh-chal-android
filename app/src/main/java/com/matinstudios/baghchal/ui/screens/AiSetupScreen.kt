package com.matinstudios.baghchal.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.engine.GOATS_TO_WIN
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.engine.ai.Difficulty
import com.matinstudios.baghchal.ui.theme.BaghchalTheme

/** Player vs AI: pick a side and a difficulty, then start. */
@Composable
fun AiSetupScreen(onStart: (GameSetup) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
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
        Text(
            stringResource(R.string.mode_vs_ai),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )

        Column(
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionLabel(R.string.home_play_as)
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SideOption(
                    selected = humanSide == Side.GOAT,
                    onClick = { humanSide = Side.GOAT },
                    icon = R.drawable.ic_goat,
                    name = stringResource(R.string.side_goats),
                    detail = stringResource(R.string.side_goats_detail),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                SideOption(
                    selected = humanSide == Side.TIGER,
                    onClick = { humanSide = Side.TIGER },
                    icon = R.drawable.ic_tiger,
                    name = stringResource(R.string.side_tigers),
                    detail = stringResource(R.string.side_tigers_detail, GOATS_TO_WIN),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }

            SectionLabel(R.string.home_difficulty)
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (level in Difficulty.entries) {
                    DifficultyOption(level, selected = difficulty == level, onClick = { difficulty = level })
                }
            }

            Button(
                onClick = { onStart(GameSetup.vsComputer(humanSide, difficulty)) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text(stringResource(R.string.ai_setup_start)) }
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.back))
            }
        }
    }
}

@Composable
private fun SectionLabel(@StringRes text: Int) {
    Text(stringResource(text), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
}

/** Outlined card that shows a thicker, coloured border when selected. */
@Composable
private fun OptionCard(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val border = if (selected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }
    OutlinedCard(
        border = border,
        modifier = modifier.selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
    ) { content() }
}

@Composable
private fun SideOption(
    selected: Boolean,
    onClick: () -> Unit,
    @DrawableRes icon: Int,
    name: String,
    detail: String,
    modifier: Modifier = Modifier,
) {
    OptionCard(selected, onClick, modifier) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(56.dp))
            Text(name, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun DifficultyOption(level: Difficulty, selected: Boolean, onClick: () -> Unit) {
    OptionCard(selected, onClick, Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            // The whole card is the radio button, so this one only shows the state.
            RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(horizontal = 12.dp))
            Column {
                Text(stringResource(level.label), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(level.detail), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private val Difficulty.detail: Int
    get() = when (this) {
        Difficulty.EASY -> R.string.difficulty_easy_detail
        Difficulty.MEDIUM -> R.string.difficulty_medium_detail
        Difficulty.HARD -> R.string.difficulty_hard_detail
    }

@Preview(showBackground = true)
@Composable
private fun AiSetupPreview() {
    BaghchalTheme { AiSetupScreen(onStart = {}, onBack = {}) }
}
