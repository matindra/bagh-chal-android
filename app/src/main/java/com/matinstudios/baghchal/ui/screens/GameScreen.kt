package com.matinstudios.baghchal.ui.screens

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Color
import com.matinstudios.baghchal.ui.theme.BaghchalThemeExtras
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.ui.haptics.CueVibrator
import com.matinstudios.baghchal.ads.BannerAd
import com.matinstudios.baghchal.ads.BannerPlacement
import com.matinstudios.baghchal.data.BoardTheme
import com.matinstudios.baghchal.engine.DrawReason
import com.matinstudios.baghchal.engine.GOATS_TO_WIN
import com.matinstudios.baghchal.engine.GameStatus
import com.matinstudios.baghchal.engine.Phase
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.engine.WinReason
import com.matinstudios.baghchal.ui.board.BoardCanvas
import com.matinstudios.baghchal.ui.theme.BaghchalTheme

@Composable
fun GameScreen(
    onMenu: () -> Unit,
    onGameWon: (GameResult) -> Unit,
    onHowToPlay: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GameViewModel = viewModel(factory = GameViewModel.Factory),
) {
    val context = LocalContext.current
    val vibrator = remember(context) { CueVibrator(context) }
    LaunchedEffect(viewModel) {
        viewModel.haptics.collect(vibrator::play)
    }
    val currentOnGameWon by rememberUpdatedState(onGameWon)
    LaunchedEffect(viewModel) {
        viewModel.results.collect { currentOnGameWon(it) }
    }
    // Null only for the moment it takes to read a saved game and settings.
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value ?: return
    val boardTheme = viewModel.boardTheme.collectAsStateWithLifecycle().value ?: return
    val showBanner by viewModel.showBanner.collectAsStateWithLifecycle()
    val undoNeedsAd by viewModel.undoNeedsAd.collectAsStateWithLifecycle()
    val hintNeedsAd by viewModel.hintNeedsAd.collectAsStateWithLifecycle()
    val menuItems = listOf(
        AppMenuItem(R.string.menu_home, R.drawable.ic_home, onMenu),
        AppMenuItem(R.string.home_how_to_play, R.drawable.ic_help, onHowToPlay),
        AppMenuItem(R.string.home_stats, R.drawable.ic_stats, onStats),
        AppMenuItem(R.string.home_settings, R.drawable.ic_settings, onSettings),
    )
    Column(modifier.fillMaxSize()) {
        AppMenuDrawer(menuItems, Modifier.weight(1f)) { openMenu ->
            GameContent(
                uiState = uiState,
                boardTheme = boardTheme,
                undoNeedsAd = undoNeedsAd,
                hintNeedsAd = hintNeedsAd,
                onPointTapped = viewModel::onPointTapped,
                onUndo = viewModel::onUndo,
                onHint = viewModel::onHint,
                onNewGame = viewModel::onNewGame,
                onMenu = onMenu,
                onOpenMenu = openMenu,
            )
        }
        // Outside the drawer so the menu never covers the ad; kept well away from the board and buttons.
        if (showBanner) BannerAd(BannerPlacement.GAME, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp))
    }
}

/**
 * @param onMenu leaves the game for Home; @param onOpenMenu opens the side menu.
 */
@Composable
fun GameContent(
    uiState: GameUiState,
    boardTheme: BoardTheme,
    undoNeedsAd: Boolean,
    hintNeedsAd: Boolean,
    onPointTapped: (Int) -> Unit,
    onUndo: () -> Unit,
    onHint: () -> Unit,
    onNewGame: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenMenu: () -> Unit = {},
) {
    val game = uiState.game
    Column(modifier.fillMaxSize()) {
        MenuTopBar(onOpenMenu) {
            // End padding matches the menu button, so the status is centred on the screen.
            Box(Modifier.weight(1f).padding(end = MenuButtonSpace), contentAlignment = Alignment.Center) {
                StatusLine(uiState)
            }
        }
        GameBody(uiState, boardTheme, undoNeedsAd, hintNeedsAd, onPointTapped, onUndo, onHint, onNewGame, onMenu)
    }
}

@Composable
private fun ColumnScope.GameBody(
    uiState: GameUiState,
    boardTheme: BoardTheme,
    undoNeedsAd: Boolean,
    hintNeedsAd: Boolean,
    onPointTapped: (Int) -> Unit,
    onUndo: () -> Unit,
    onHint: () -> Unit,
    onNewGame: () -> Unit,
    onMenu: () -> Unit,
) {
    val game = uiState.game
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        BoardCanvas(
            board = game.board,
            theme = boardTheme,
            selected = uiState.selected,
            targets = uiState.targets,
            trappedTigers = uiState.trappedTigers,
            lastMove = uiState.lastMove,
            moveNumber = uiState.moveNumber,
            hint = uiState.hint,
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
        // Wins move on to the result screen; only draws end here.
        if (uiState.status is GameStatus.Drawn) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onMenu) { Text(stringResource(R.string.menu)) }
                Button(onClick = onNewGame) { Text(stringResource(R.string.new_game)) }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AssistButton(
                    onClick = onUndo,
                    enabled = uiState.canUndo,
                    icon = R.drawable.ic_undo,
                    text = assistLabel(R.string.undo, undoNeedsAd),
                    container = MaterialTheme.colorScheme.tertiaryContainer,
                    content = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                AssistButton(
                    onClick = onHint,
                    enabled = uiState.canHint,
                    icon = R.drawable.ic_hint,
                    text = assistLabel(R.string.hint, hintNeedsAd),
                    container = BaghchalThemeExtras.colors.hintContainer,
                    content = BaghchalThemeExtras.colors.onHintContainer,
                )
            }
        }
    }
}

/** Undo / Hint: a tinted button with a leading icon. */
@Composable
private fun AssistButton(
    onClick: () -> Unit,
    enabled: Boolean,
    @DrawableRes icon: Int,
    text: String,
    container: Color,
    content: Color,
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = container, contentColor = content),
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
    ) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(text)
    }
}

/** Says up front when a button plays a rewarded ad, as AdMob requires. */
@Composable
private fun assistLabel(@StringRes label: Int, needsAd: Boolean): String =
    if (needsAd) stringResource(R.string.with_ad, stringResource(label)) else stringResource(label)

@Composable
private fun StatusLine(uiState: GameUiState, modifier: Modifier = Modifier) {
    val status = uiState.status
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        val headline = when (status) {
            GameStatus.InProgress -> stringResource(
                if (uiState.game.sideToMove == Side.TIGER) R.string.turn_tigers else R.string.turn_goats,
            )
            else -> resultText(status)
        }
        if (status == GameStatus.InProgress) {
            TurnBadge(uiState.game.sideToMove, headline)
        } else {
            Text(headline, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Row(
            modifier = Modifier.height(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (uiState.isAiThinking || uiState.isHintLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Text(
                    stringResource(if (uiState.isAiThinking) R.string.ai_thinking else R.string.hint_loading),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(subtitle(uiState), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Whose turn, in that side's colour with its piece: orange for tigers, green for goats. */
@Composable
private fun TurnBadge(side: Side, text: String) {
    val colors = MaterialTheme.colorScheme
    val isTiger = side == Side.TIGER
    val container by animateColorAsState(if (isTiger) colors.primaryContainer else colors.secondaryContainer, label = "turn")
    val content by animateColorAsState(if (isTiger) colors.onPrimaryContainer else colors.onSecondaryContainer, label = "turn text")
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(container)
            .padding(start = 8.dp, end = 20.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Image(
            painterResource(if (isTiger) R.drawable.ic_tiger else R.drawable.ic_goat),
            contentDescription = null,
            modifier = Modifier.size(36.dp),
        )
        Text(text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = content)
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
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(start = 6.dp, end = 14.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(32.dp))
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Preview(showBackground = true)
@Composable
private fun GameContentPreview() {
    BaghchalTheme {
        GameContent(
            uiState = GameInteraction.newGame(),
            boardTheme = BoardTheme.WOOD,
            undoNeedsAd = true,
            hintNeedsAd = true,
            onPointTapped = {},
            onUndo = {},
            onHint = {},
            onNewGame = {},
            onMenu = {},
        )
    }
}
