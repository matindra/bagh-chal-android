package com.matinstudios.baghchal.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.engine.Side

private val SelectedColor = Color(0xFFFFD54F)
private val TargetColor = Color(0xB34CAF50)

/** Square board with pieces; reports taps as engine point indices. */
@Composable
fun BoardCanvas(
    board: List<Side?>,
    selected: Int?,
    targets: Set<Int>,
    onPointTapped: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val boardPainter = painterResource(R.drawable.board_wood)
    val tigerPainter = painterResource(R.drawable.ic_tiger)
    val goatPainter = painterResource(R.drawable.ic_goat)
    val currentOnPointTapped by rememberUpdatedState(onPointTapped)
    val description = stringResource(R.string.cd_board)

    Canvas(
        modifier
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    BoardGeometry.pointAt(offset, size.width.toFloat())?.let(currentOnPointTapped)
                }
            },
    ) {
        val boardSize = size.minDimension
        val scale = BoardGeometry.scale(boardSize)
        with(boardPainter) { draw(Size(boardSize, boardSize)) }

        for (point in targets) {
            drawCircle(TargetColor, radius = 24f * scale, center = BoardGeometry.pointCenter(point, boardSize))
        }

        val pieceSize = BoardGeometry.PIECE_SIZE * scale
        board.forEachIndexed { point, side ->
            if (side == null) return@forEachIndexed
            val center = BoardGeometry.pointCenter(point, boardSize)
            if (point == selected) {
                drawCircle(
                    SelectedColor,
                    radius = pieceSize / 2 + 2f * scale,
                    center = center,
                    style = Stroke(width = 7f * scale),
                )
            }
            val painter = if (side == Side.TIGER) tigerPainter else goatPainter
            translate(center.x - pieceSize / 2, center.y - pieceSize / 2) {
                with(painter) { draw(Size(pieceSize, pieceSize)) }
            }
        }
    }
}
