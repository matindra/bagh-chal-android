package com.matinstudios.baghchal.ui.board

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

private val ArrowFill = Color(0xFFFFD54F)
private val ArrowOutline = Color(0xD93A2416)

/** Draws [arrow] (from [MoveArrow.shape]); [scale] is the board's px per art unit. */
fun DrawScope.drawMoveArrow(arrow: ArrowShape, scale: Float) {
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
