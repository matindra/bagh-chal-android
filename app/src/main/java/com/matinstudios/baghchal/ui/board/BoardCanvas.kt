package com.matinstudios.baghchal.ui.board

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.data.BoardTheme
import com.matinstudios.baghchal.engine.Board
import com.matinstudios.baghchal.engine.Move
import com.matinstudios.baghchal.engine.Side
import kotlin.math.roundToInt

private val SelectedColor = Color(0xFFFFD54F)

/** Lock badge on a trapped tiger: size and offset of its centre to the top-right, relative to the piece. */
private const val LOCK_BADGE_SIZE = 0.42f
private const val LOCK_BADGE_OFFSET = 0.36f
private val TargetColor = Color(0xB34CAF50)

/**
 * Square board with pieces; reports taps as engine point indices. [lastMove] is animated once
 * each time [moveNumber] changes. Each point is also its own accessibility node (see [BoardPointNodes]).
 */
@Composable
fun BoardCanvas(
    board: List<Side?>,
    theme: BoardTheme,
    selected: Int?,
    targets: Set<Int>,
    trappedTigers: Set<Int>,
    lastMove: Move?,
    moveNumber: Int,
    onPointTapped: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val boardPainter = painterResource(theme.drawable)
    val tigerPainter = painterResource(R.drawable.ic_tiger)
    val goatPainter = painterResource(R.drawable.ic_goat)
    val lockPainter = painterResource(R.drawable.lock_badge)
    val currentOnPointTapped by rememberUpdatedState(onPointTapped)
    val description = stringResource(R.string.cd_board)

    val progress = remember { Animatable(1f) }
    // Saved so a recreated screen (e.g. tablet rotation) doesn't replay the last move.
    var animatedMoveNumber by rememberSaveable { mutableIntStateOf(moveNumber) }
    LaunchedEffect(moveNumber) {
        if (moveNumber == animatedMoveNumber) return@LaunchedEffect
        animatedMoveNumber = moveNumber
        val move = lastMove ?: return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(1f, tween(PieceMotion.durationMillis(move), easing = FastOutSlowInEasing))
    }

    BoxWithConstraints(modifier) {
        Canvas(
            Modifier
                .fillMaxSize()
                .semantics { contentDescription = description }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        BoardGeometry.pointAt(offset, minOf(size.width, size.height).toFloat())?.let(currentOnPointTapped)
                    }
                },
        ) {
            val boardSize = size.minDimension
            val scale = BoardGeometry.scale(boardSize)
            val pieceSize = BoardGeometry.PIECE_SIZE * scale
            val p = progress.value
            val moving = lastMove?.takeIf { p < 1f }

            with(boardPainter) { draw(Size(boardSize, boardSize)) }

            // Targets appear once the move has landed, so they don't show through a fading goat.
            if (moving == null) for (point in targets) {
                drawCircle(TargetColor, radius = 24f * scale, center = BoardGeometry.pointCenter(point, boardSize))
            }

            board.forEachIndexed { point, side ->
                if (side == null || point == moving?.to) return@forEachIndexed
                val center = BoardGeometry.pointCenter(point, boardSize)
                if (point == selected) {
                    drawCircle(
                        SelectedColor,
                        radius = pieceSize / 2 + 2f * scale,
                        center = center,
                        style = Stroke(width = 7f * scale),
                    )
                }
                drawPiece(if (side == Side.TIGER) tigerPainter else goatPainter, center, pieceSize)
                if (point in trappedTigers) {
                    val offset = pieceSize * LOCK_BADGE_OFFSET
                    drawPiece(lockPainter, center + Offset(offset, -offset), pieceSize * LOCK_BADGE_SIZE)
                }
            }

            if (moving is Move.Jump) {
                val over = BoardGeometry.pointCenter(moving.over, boardSize)
                drawPiece(goatPainter, over, pieceSize, alpha = PieceMotion.capturedAlpha(p))
            }
            // The moving piece goes last so it passes over everything else.
            val movingSide = moving?.let { board[it.to] }
            if (moving != null && movingSide != null) {
                drawPiece(
                    painter = if (movingSide == Side.TIGER) tigerPainter else goatPainter,
                    center = PieceMotion.center(moving, p, boardSize),
                    size = pieceSize * PieceMotion.scale(moving, p),
                    alpha = PieceMotion.alpha(moving, p),
                )
            }
        }
        // Above the canvas: accessibility drops nodes that a sibling drawn on top fully covers.
        BoardPointNodes(
            board = board,
            selectedPoint = selected,
            targets = targets,
            trappedTigers = trappedTigers,
            boardSizePx = minOf(constraints.maxWidth, constraints.maxHeight).toFloat(),
            onPointTapped = onPointTapped,
        )
    }
}

/** Test tag of the accessibility node for [point]. */
fun boardPointTestTag(point: Int) = "board_point_$point"

/**
 * One invisible node per point, in reading order, so TalkBack can announce and activate points
 * individually ("Row 2, column 3, goat"). They have no pointer input: ordinary taps fall through
 * to the canvas below.
 */
@Composable
private fun BoardPointNodes(
    board: List<Side?>,
    selectedPoint: Int?,
    targets: Set<Int>,
    trappedTigers: Set<Int>,
    boardSizePx: Float,
    onPointTapped: (Int) -> Unit,
) {
    val cellPx = BoardGeometry.SPACING * BoardGeometry.scale(boardSizePx)
    val cellDp = with(LocalDensity.current) { cellPx.toDp() }
    val possibleMove = stringResource(R.string.point_possible_move)
    for (point in 0 until Board.POINTS) {
        val piece = stringResource(
            when (board[point]) {
                Side.GOAT -> R.string.point_goat
                Side.TIGER -> if (point in trappedTigers) R.string.point_tiger_trapped else R.string.point_tiger
                null -> R.string.point_empty
            },
        )
        val description = stringResource(R.string.point_description, Board.row(point) + 1, Board.col(point) + 1, piece)
        val center = BoardGeometry.pointCenter(point, boardSizePx)
        Box(
            Modifier
                .offset { IntOffset((center.x - cellPx / 2).roundToInt(), (center.y - cellPx / 2).roundToInt()) }
                .size(cellDp)
                .testTag(boardPointTestTag(point))
                .semantics {
                    contentDescription = description
                    role = Role.Button
                    selected = point == selectedPoint
                    if (point in targets) stateDescription = possibleMove
                    onClick {
                        onPointTapped(point)
                        true
                    }
                },
        )
    }
}

@get:DrawableRes
val BoardTheme.drawable: Int
    get() = when (this) {
        BoardTheme.WOOD -> R.drawable.board_wood
        BoardTheme.STONE -> R.drawable.board_stone
        BoardTheme.CLOTH -> R.drawable.board_cloth
    }

private fun DrawScope.drawPiece(painter: Painter, center: Offset, size: Float, alpha: Float = 1f) {
    translate(center.x - size / 2, center.y - size / 2) {
        with(painter) { draw(Size(size, size), alpha = alpha) }
    }
}
