package com.matinstudios.baghchal.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.matinstudios.baghchal.engine.DrawReason
import com.matinstudios.baghchal.engine.GOATS_TO_WIN
import com.matinstudios.baghchal.engine.GameStatus
import com.matinstudios.baghchal.engine.Phase
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.engine.WinReason
import com.matinstudios.baghchal.ui.board.BoardCanvas
import com.matinstudios.baghchal.ui.theme.BaghchalTheme

@Composable
fun GameScreen(onMenu: () -> Unit, modifier: Modifier = Modifier, viewModel: GameViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    GameContent(
        uiState = uiState,
        onPointTapped = viewModel::onPointTapped,
        onNewGame = viewModel::onNewGame,
        onMenu = onMenu,
        modifier = modifier,
    )
}

@Composable
fun GameContent(
    uiState: GameUiState,
    onPointTapped: (Int) -> Unit,
    onNewGame: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val game = uiState.game
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        StatusLine(uiState)
        BoardCanvas(
            board = game.board,
            selected = uiState.selected,
            targets = uiState.targets,
            onPointTapped = onPointTapped,
            modifier = Modifier
                .widthIn(max = 600.dp)
                .weight(1f, fill = false)
                .aspectRatio(1f),
        )
        Row(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Counter(R.drawable.ic_goat, stringResource(R.string.goats_in_hand, game.goatsInHand))
            Counter(R.drawable.ic_tiger, stringResource(R.string.goats_captured, game.goatsCaptured, GOATS_TO_WIN))
        }
        if (uiState.status != GameStatus.InProgress) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onMenu) { Text(stringResource(R.string.menu)) }
                Button(onClick = onNewGame) { Text(stringResource(R.string.new_game)) }
            }
        } else {
            // Keeps the layout from jumping when the buttons appear.
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun StatusLine(uiState: GameUiState) {
    val status = uiState.status
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val headline = when (status) {
            GameStatus.InProgress -> stringResource(
                if (uiState.game.sideToMove == Side.TIGER) R.string.turn_tigers else R.string.turn_goats,
            )
            else -> resultText(status)
        }
        Text(headline, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.height(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (uiState.isAiThinking) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Text(stringResource(R.string.ai_thinking), style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(subtitle(uiState), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun subtitle(uiState: GameUiState): String {
    val status = uiState.status
    val aiSide = uiState.aiSide
    return when {
        status == GameStatus.InProgress -> stringResource(
            if (uiState.game.phase == Phase.PLACEMENT) R.string.phase_placement else R.string.phase_movement,
        )
        aiSide == null || status is GameStatus.Drawn -> stringResource(R.string.game_over)
        status is GameStatus.Won && status.winner == aiSide -> stringResource(R.string.result_you_lose)
        else -> stringResource(R.string.result_you_win)
    }
}

@Composable
private fun resultText(status: GameStatus): String = when (status) {
    GameStatus.InProgress -> ""
    is GameStatus.Won -> when (status.reason) {
        WinReason.CAPTURES -> stringResource(R.string.result_tigers_win_captures, GOATS_TO_WIN)
        WinReason.TIGERS_TRAPPED -> stringResource(R.string.result_goats_win_trapped)
        WinReason.GOATS_BLOCKED -> stringResource(R.string.result_tigers_win_blocked)
    }
    is GameStatus.Drawn -> when (status.reason) {
        DrawReason.REPETITION -> stringResource(R.string.result_draw_repetition)
        DrawReason.MOVE_LIMIT -> stringResource(R.string.result_draw_move_limit)
    }
}

@Composable
private fun Counter(@DrawableRes icon: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(32.dp))
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Preview(showBackground = true)
@Composable
private fun GameContentPreview() {
    BaghchalTheme {
        GameContent(uiState = GameInteraction.newGame(), onPointTapped = {}, onNewGame = {}, onMenu = {})
    }
}
