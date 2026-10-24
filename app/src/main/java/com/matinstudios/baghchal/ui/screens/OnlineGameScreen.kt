package com.matinstudios.baghchal.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.ads.BannerAd
import com.matinstudios.baghchal.ads.BannerPlacement
import com.matinstudios.baghchal.engine.Side
import kotlinx.coroutines.delay

private const val END_DIALOG_DELAY_MILLIS = 1_200L

/**
 * An online game. Leaving before the end (Back, or Home in the menu) asks first, because it concedes
 * the game. How to play, Stats and Settings open on top while the game carries on.
 */
@Composable
fun OnlineGameScreen(
    onHome: () -> Unit,
    onPlayAgain: () -> Unit,
    onHowToPlay: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnlineGameViewModel = viewModel(factory = OnlineGameViewModel.Factory),
) {
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(viewModel) {
        viewModel.haptics.collect { cue -> haptics.performHapticFeedback(cue.hapticType) }
    }
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val boardTheme by viewModel.boardTheme.collectAsStateWithLifecycle()
    val showBanner by viewModel.showBanner.collectAsStateWithLifecycle()
    var confirmLeave by rememberSaveable { mutableStateOf(false) }

    val end = ui.end
    val leave = {
        if (end == null) {
            confirmLeave = true
        } else {
            viewModel.onLeaveFinished()
            onHome()
        }
    }
    BackHandler(onBack = leave)

    val menuItems = listOf(
        AppMenuItem(R.string.menu_home, R.drawable.ic_home, leave),
        AppMenuItem(R.string.home_how_to_play, R.drawable.ic_help, onHowToPlay),
        AppMenuItem(R.string.home_stats, R.drawable.ic_stats, onStats),
        AppMenuItem(R.string.home_settings, R.drawable.ic_settings, onSettings),
    )
    Column(modifier.fillMaxSize()) {
        val session = ui.session
        val theme = boardTheme
        if (session == null || theme == null) {
            Connecting(Modifier.weight(1f))
        } else {
            AppMenuDrawer(menuItems, Modifier.weight(1f)) { openMenu ->
                GameContent(
                    uiState = session.ui,
                    boardTheme = theme,
                    undoNeedsAd = false,
                    hintNeedsAd = false,
                    onPointTapped = viewModel::onPointTapped,
                    onUndo = {},
                    onHint = {},
                    onNewGame = {},
                    onMenu = leave,
                    onOpenMenu = openMenu,
                    localControls = false,
                    statusNote = statusNote(ui, session),
                )
            }
        }
        // Outside the drawer so the menu never covers the ad; kept well away from the board.
        if (showBanner) BannerAd(BannerPlacement.GAME, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp))
    }

    if (confirmLeave && end == null) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text(stringResource(R.string.online_leave_title)) },
            text = { Text(stringResource(R.string.online_leave_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmLeave = false
                    viewModel.onResign()
                    onHome()
                }) { Text(stringResource(R.string.online_leave)) }
            },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text(stringResource(R.string.online_stay)) } },
        )
    }

    // A short pause first, so the final move and any lock badges can be seen.
    var showEnd by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(end) {
        if (end != null && !showEnd) {
            delay(END_DIALOG_DELAY_MILLIS)
            showEnd = true
        }
    }
    if (end != null && showEnd) {
        EndDialog(
            end = end,
            onPlayAgain = {
                viewModel.onLeaveFinished()
                onPlayAgain()
            },
            onHome = {
                viewModel.onLeaveFinished()
                onHome()
            },
        )
    }
}

@Composable
private fun statusNote(ui: OnlineGameUi, session: OnlineSession): String {
    val side = stringResource(if (session.mySide == Side.GOAT) R.string.side_goats else R.string.side_tigers)
    val turn = when {
        ui.opponentAway -> stringResource(R.string.online_opponent_away)
        session.ui.game.sideToMove == session.mySide -> stringResource(R.string.online_your_turn)
        else -> stringResource(R.string.online_their_turn)
    }
    return "${stringResource(R.string.online_you_play, side)} · $turn"
}

@Composable
private fun Connecting(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        CircularProgressIndicator()
        Text(stringResource(R.string.online_connecting), style = MaterialTheme.typography.titleMedium)
    }
}

/** The result. Not dismissable by tapping outside: the player picks what's next. */
@Composable
private fun EndDialog(end: OnlineEnd, onPlayAgain: () -> Unit, onHome: () -> Unit) {
    val title = when (end.outcome) {
        OnlineOutcome.WON -> R.string.online_won
        OnlineOutcome.LOST -> R.string.online_lost
        OnlineOutcome.DRAWN -> R.string.online_drawn
    }
    val detail = when (end.reason) {
        OnlineEndReason.RULES -> when (end.outcome) {
            OnlineOutcome.WON -> R.string.online_reason_rules_won
            OnlineOutcome.LOST -> R.string.online_reason_rules_lost
            OnlineOutcome.DRAWN -> R.string.online_reason_rules_drawn
        }
        OnlineEndReason.RESIGNED -> R.string.online_reason_resigned
        OnlineEndReason.OPPONENT_RESIGNED -> R.string.online_reason_opponent_resigned
        OnlineEndReason.OPPONENT_LEFT -> R.string.online_reason_opponent_left
        OnlineEndReason.OPPONENT_INVALID_MOVE -> R.string.online_reason_invalid
    }
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(title)) },
        text = { Text(stringResource(detail)) },
        confirmButton = { TextButton(onClick = onPlayAgain) { Text(stringResource(R.string.online_play_again)) } },
        dismissButton = { TextButton(onClick = onHome) { Text(stringResource(R.string.menu_home)) } },
    )
}
