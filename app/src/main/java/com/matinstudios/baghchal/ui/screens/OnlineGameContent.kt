package com.matinstudios.baghchal.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.data.BoardTheme
import com.matinstudios.baghchal.engine.GOATS_TO_WIN
import com.matinstudios.baghchal.engine.GameStatus
import com.matinstudios.baghchal.engine.Phase
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.engine.TOTAL_GOATS
import com.matinstudios.baghchal.ui.board.BoardCanvas
import com.matinstudios.baghchal.ui.theme.BaghchalTheme

/**
 * The online board: the opponent's bar above it, a turn banner and this player's bar below.
 * The bar of the side to move is highlighted in that side's colours.
 */
@Composable
fun OnlineGameContent(
    session: OnlineSession,
    opponentAway: Boolean,
    end: OnlineEnd?,
    boardTheme: BoardTheme,
    onPointTapped: (Int) -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val game = session.ui.game
    Column(modifier.fillMaxSize()) {
        MenuTopBar(onOpenMenu) {
            Column(Modifier.padding(start = 4.dp)) {
                Text(
                    stringResource(R.string.online_game_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
                val phase = stringResource(if (game.phase == Phase.PLACEMENT) R.string.phase_placement else R.string.phase_movement)
                Text(
                    stringResource(R.string.online_move_number, phase, game.moveCount + 1),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        OnlineGameBody(session, opponentAway, end, boardTheme, onPointTapped)
    }
}

@Composable
private fun ColumnScope.OnlineGameBody(
    session: OnlineSession,
    opponentAway: Boolean,
    end: OnlineEnd?,
    boardTheme: BoardTheme,
    onPointTapped: (Int) -> Unit,
) {
    val uiState = session.ui
    val game = uiState.game
    val mySide = session.mySide
    val playing = end == null && uiState.status == GameStatus.InProgress
    val myTurn = playing && game.sideToMove == mySide
    val theirTurn = playing && !myTurn
    Column(
        modifier = Modifier.weight(1f).fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        val barWidth = Modifier.widthIn(max = 600.dp).fillMaxWidth()
        PlayerBar(
            side = mySide.opponent,
            isYou = false,
            active = theirTurn,
            thinking = theirTurn && !opponentAway,
            away = opponentAway && end == null,
            goatsInHand = game.goatsInHand,
            goatsCaptured = game.goatsCaptured,
            modifier = barWidth,
        )
        BoardCanvas(
            board = game.board,
            theme = boardTheme,
            selected = uiState.selected,
            targets = uiState.targets,
            trappedTigers = uiState.trappedTigers,
            lastMove = uiState.lastMove,
            moveNumber = uiState.moveNumber,
            onPointTapped = onPointTapped,
            modifier = Modifier
                .widthIn(max = 600.dp)
                .weight(1f, fill = false)
                .aspectRatio(1f),
        )
        TurnBanner(
            text = turnText(end, opponentAway, myTurn, mySide, game.phase),
            highlight = if (myTurn) mySide else null,
            modifier = barWidth,
        )
        PlayerBar(
            side = mySide,
            isYou = true,
            active = myTurn,
            thinking = false,
            away = false,
            goatsInHand = game.goatsInHand,
            goatsCaptured = game.goatsCaptured,
            modifier = barWidth,
        )
    }
}

@Composable
private fun turnText(end: OnlineEnd?, opponentAway: Boolean, myTurn: Boolean, mySide: Side, phase: Phase): String = when {
    end != null -> stringResource(end.outcome.title)
    opponentAway -> stringResource(R.string.online_opponent_away)
    !myTurn -> stringResource(R.string.online_their_turn)
    mySide == Side.TIGER -> stringResource(R.string.online_turn_move_tiger)
    phase == Phase.PLACEMENT -> stringResource(R.string.online_turn_place_goat)
    else -> stringResource(R.string.online_turn_move_goat)
}

internal val OnlineOutcome.title: Int
    get() = when (this) {
        OnlineOutcome.WON -> R.string.online_won
        OnlineOutcome.LOST -> R.string.online_lost
        OnlineOutcome.DRAWN -> R.string.online_drawn
    }

/** One player: their piece, who they are, and their side's count (captures for tigers, goats for goats). */
@Composable
private fun PlayerBar(
    side: Side,
    isYou: Boolean,
    active: Boolean,
    thinking: Boolean,
    away: Boolean,
    goatsInHand: Int,
    goatsCaptured: Int,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val isTiger = side == Side.TIGER
    val sideContainer = if (isTiger) colors.primaryContainer else colors.secondaryContainer
    val onSideContainer = if (isTiger) colors.onPrimaryContainer else colors.onSecondaryContainer
    val accent = if (isTiger) colors.primary else colors.secondary
    val container by animateColorAsState(if (active) sideContainer else colors.surfaceContainer, label = "player bar")
    val content by animateColorAsState(if (active) onSideContainer else colors.onSurface, label = "player bar text")
    val shape = RoundedCornerShape(20.dp)

    val sideName = stringResource(if (isTiger) R.string.side_tigers else R.string.side_goats)
    val detail = when {
        away -> stringResource(R.string.online_player_away, sideName)
        thinking -> stringResource(R.string.online_player_thinking, sideName)
        else -> sideName
    }
    Row(
        modifier = modifier
            .height(64.dp)
            .clip(shape)
            .background(container)
            .border(if (active) BorderStroke(2.dp, accent) else BorderStroke(0.dp, Color.Transparent), shape)
            .semantics(mergeDescendants = true) {}
            .padding(start = 10.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PieceCircle(
            if (isTiger) R.drawable.ic_tiger else R.drawable.ic_goat,
            44.dp,
            if (active) colors.surface else sideContainer,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(if (isYou) R.string.online_player_you else R.string.online_player_opponent),
                style = MaterialTheme.typography.titleMedium,
                color = content,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (thinking) CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 1.5.dp, color = content)
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (away) colors.error else content.copy(alpha = 0.8f),
                )
            }
        }
        if (isTiger) CaptureCount(goatsCaptured, accent, content) else GoatCount(goatsInHand, goatsCaptured, content)
    }
}

@Composable
private fun CaptureCount(captured: Int, accent: Color, content: Color) {
    val label = stringResource(R.string.online_captured, captured, GOATS_TO_WIN)
    Column(
        modifier = Modifier.clearAndSetSemantics { contentDescription = label },
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = content)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(GOATS_TO_WIN) { i ->
                val dot = Modifier.size(10.dp).clip(CircleShape)
                Box(
                    if (i < captured) {
                        dot.background(accent)
                    } else {
                        dot.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    },
                )
            }
        }
    }
}

/** Goats in hand while placing, then how many are left on the board. */
@Composable
private fun GoatCount(inHand: Int, captured: Int, content: Color) {
    val placing = inHand > 0
    val count = if (placing) inHand else TOTAL_GOATS - captured
    val label = stringResource(if (placing) R.string.online_in_hand else R.string.online_on_board)
    Column(
        modifier = Modifier.clearAndSetSemantics { contentDescription = "$count $label" },
        horizontalAlignment = Alignment.End,
    ) {
        Text(count.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = content)
        Text(label, style = MaterialTheme.typography.labelMedium, color = content.copy(alpha = 0.8f))
    }
}

/** Whose turn it is; solid in this player's side colour when it's their move. */
@Composable
private fun TurnBanner(text: String, highlight: Side?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(
        when (highlight) {
            Side.TIGER -> colors.primary
            Side.GOAT -> colors.secondary
            null -> colors.surfaceContainerHigh
        },
        label = "turn banner",
    )
    val content by animateColorAsState(
        when (highlight) {
            Side.TIGER -> colors.onPrimary
            Side.GOAT -> colors.onSecondary
            null -> colors.onSurfaceVariant
        },
        label = "turn banner text",
    )
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    ) {
        if (highlight != null) Box(Modifier.size(8.dp).clip(CircleShape).background(content.copy(alpha = 0.7f)))
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = content,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@Preview(showBackground = true, heightDp = 780, widthDp = 390)
@Composable
private fun OnlineGameContentPreview() {
    BaghchalTheme {
        OnlineGameContent(
            session = OnlineSession.start(Side.GOAT, emptyList()),
            opponentAway = false,
            end = null,
            boardTheme = BoardTheme.WOOD,
            onPointTapped = {},
            onOpenMenu = {},
        )
    }
}
