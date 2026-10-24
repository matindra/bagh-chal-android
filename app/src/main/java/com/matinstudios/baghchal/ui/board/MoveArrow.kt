package com.matinstudios.baghchal.ui.board

import androidx.compose.ui.geometry.Offset
import com.matinstudios.baghchal.engine.Move

/** A curved arrow: a quadratic curve from [start] to [end] through [control], with a triangular head at [end]. */
data class ArrowShape(val start: Offset, val control: Offset, val end: Offset, val headLeft: Offset, val headRight: Offset)

/** Where to draw the arrow for a move in a rules illustration. Pure so it can be unit-tested. */
object MoveArrow {
    /** How far a capture arrow arcs away from the straight line, in board units. */
    const val JUMP_LIFT = 70f

    private const val START_INSET = 0.3f // × piece radius: start inside the faded origin piece
    private const val END_INSET = 0.8f // × piece radius: stop at the edge of the piece that arrived
    private const val HEAD_LENGTH = 26f
    private const val HEAD_HALF_WIDTH = 17f
    private const val HAND_EDGE = 0.98f // a placed goat comes in from the board's bottom edge

    fun shape(move: Move, boardSizePx: Float): ArrowShape {
        val scale = BoardGeometry.scale(boardSizePx)
        val pieceRadius = BoardGeometry.PIECE_SIZE / 2 * scale
        val to = BoardGeometry.pointCenter(move.to, boardSizePx)
        val from = when (move) {
            is Move.Place -> Offset(to.x, boardSizePx * HAND_EDGE)
            is Move.Step -> BoardGeometry.pointCenter(move.from, boardSizePx)
            is Move.Jump -> BoardGeometry.pointCenter(move.from, boardSizePx)
        }
        val midpoint = (from + to) / 2f
        val control = if (move is Move.Jump) midpoint + liftDirection(from, to) * (JUMP_LIFT * scale) else midpoint

        val start = if (move is Move.Place) from else from + direction(from, control) * (pieceRadius * START_INSET)
        val end = to + direction(to, control) * (pieceRadius * END_INSET)

        val forward = direction(control, end)
        val side = Offset(-forward.y, forward.x)
        val base = end - forward * (HEAD_LENGTH * scale)
        return ArrowShape(
            start = start,
            control = control,
            end = end,
            headLeft = base + side * (HEAD_HALF_WIDTH * scale),
            headRight = base - side * (HEAD_HALF_WIDTH * scale),
        )
    }

    /** Unit vector perpendicular to from→to, pointing up (or left for a vertical line). */
    private fun liftDirection(from: Offset, to: Offset): Offset {
        val d = direction(from, to)
        val normal = Offset(d.y, -d.x)
        val pointsAway = normal.y < 0f || (normal.y == 0f && normal.x < 0f)
        return if (pointsAway) normal else -normal
    }

    private fun direction(from: Offset, to: Offset): Offset {
        val d = to - from
        val length = d.getDistance()
        return if (length == 0f) Offset.Zero else d / length
    }
}
