package com.matinstudios.baghchal.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.data.BoardTheme
import com.matinstudios.baghchal.engine.Move
import com.matinstudios.baghchal.ui.screens.RuleIllustration

private val ArrowFill = Color(0xFFFFD54F)
private val ArrowOutline = Color(0xD93A2416)
private const val GHOST_ALPHA = 0.35f

/** Static mini board for the rules: the position after the move, the origin faded, and an arrow. */
@Composable
fun RuleBoard(illustration: RuleIllustration, theme: BoardTheme, modifier: Modifier = Modifier) {
    val boardPainter = painterResource(theme.drawable)
    val tigerPainter = painterResource(R.drawable.ic_tiger)
    val goatPainter = painterResource(R.drawable.ic_goat)
    val lockPainter = painterResource(R.drawable.lock_badge)
    val tigers = illustration.tigersAfter
    val goats = illustration.goatsAfter
    val trapped = illustration.trappedAfter
    val move = illustration.move

    Canvas(modifier) {
        val boardSize = size.minDimension
        val scale = BoardGeometry.scale(boardSize)
        val pieceSize = BoardGeometry.PIECE_SIZE * scale
        fun center(point: Int) = BoardGeometry.pointCenter(point, boardSize)

        with(boardPainter) { draw(Size(boardSize, boardSize)) }

        when (move) {
            is Move.Place -> Unit
            is Move.Step -> {
                val ghost = if (move.to in tigers) tigerPainter else goatPainter
                drawPiece(ghost, center(move.from), pieceSize, GHOST_ALPHA)
            }
            is Move.Jump -> {
                drawPiece(tigerPainter, center(move.from), pieceSize, GHOST_ALPHA)
                drawPiece(goatPainter, center(move.over), pieceSize, GHOST_ALPHA)
            }
        }
        for (point in goats) drawPiece(goatPainter, center(point), pieceSize)
        for (point in tigers) {
            drawPiece(tigerPainter, center(point), pieceSize)
            if (point in trapped) {
                val offset = pieceSize * 0.36f
                drawPiece(lockPainter, center(point) + Offset(offset, -offset), pieceSize * 0.42f)
            }
        }

        drawArrow(MoveArrow.shape(move, boardSize), scale)
    }
}

private fun DrawScope.drawArrow(arrow: ArrowShape, scale: Float) {
    val shaft = Path().apply {
        moveTo(arrow.start.x, arrow.start.y)
        quadraticTo(arrow.control.x, arrow.control.y, arrow.end.x, arrow.end.y)
    }
    val head = Path().apply {
        moveTo(arrow.end.x, arrow.end.y)
        lineTo(arrow.headLeft.x, arrow.headLeft.y)
        lineTo(arrow.headRight.x, arrow.headRight.y)
        close()
    }
    // Dark outline first so the arrow reads on every board theme.
    drawPath(shaft, ArrowOutline, style = Stroke(width = 14f * scale, cap = StrokeCap.Round))
    drawPath(head, ArrowOutline, style = Stroke(width = 8f * scale, join = StrokeJoin.Round))
    drawPath(head, ArrowOutline)
    drawPath(shaft, ArrowFill, style = Stroke(width = 8f * scale, cap = StrokeCap.Round))
    drawPath(head, ArrowFill)
}

private fun DrawScope.drawPiece(painter: Painter, center: Offset, size: Float, alpha: Float = 1f) {
    translate(center.x - size / 2, center.y - size / 2) {
        with(painter) { draw(Size(size, size), alpha = alpha) }
    }
}
