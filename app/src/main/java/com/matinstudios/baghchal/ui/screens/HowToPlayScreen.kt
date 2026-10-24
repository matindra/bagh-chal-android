package com.matinstudios.baghchal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.data.BoardTheme
import com.matinstudios.baghchal.ui.board.RuleBoard
import com.matinstudios.baghchal.ui.theme.BaghchalTheme

@Composable
fun HowToPlayScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HowToPlayViewModel = viewModel(factory = HowToPlayViewModel.Factory),
) {
    val theme = viewModel.boardTheme.collectAsStateWithLifecycle().value ?: return
    HowToPlayContent(theme, onBack, modifier)
}

@Composable
fun HowToPlayContent(boardTheme: BoardTheme, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(R.string.rules_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        Text(stringResource(R.string.rules_goal), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)

        HOW_TO_PLAY_STEPS.forEachIndexed { index, step ->
            Card(Modifier.widthIn(max = 480.dp).fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.rules_step_title, index + 1, stringResource(step.title)),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() },
                    )
                    RulePicture(step, boardTheme, Modifier.fillMaxWidth())
                    Text(stringResource(step.body), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        Text(
            stringResource(R.string.rules_draws),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 480.dp),
        )
        OutlinedButton(onClick = onBack) { Text(stringResource(R.string.back)) }
    }
}

/** The step's mini board, described for screen readers. */
@Composable
internal fun RulePicture(step: HowToPlayStep, boardTheme: BoardTheme, modifier: Modifier = Modifier) {
    val picture = stringResource(step.illustrationDescription)
    RuleBoard(
        illustration = step.illustration,
        theme = boardTheme,
        modifier = modifier
            .aspectRatio(1f)
            .semantics { contentDescription = picture },
    )
}

@Preview(showBackground = true, heightDp = 2000)
@Composable
private fun HowToPlayPreview() {
    BaghchalTheme { HowToPlayContent(BoardTheme.WOOD, onBack = {}) }
}
