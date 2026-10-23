package com.matinstudios.baghchal.ui.board

import androidx.compose.ui.geometry.Offset
import com.matinstudios.baghchal.engine.Board

/**
 * Maps engine points to positions on the board artwork. All board drawables are 600×600 units
 * with points at 75, 187.5, 300, 412.5, 525 on both axes.
 */
object BoardGeometry {
    const val UNITS = 600f
    const val FIRST_POINT = 75f
    const val SPACING = 112.5f
    const val PIECE_SIZE = 90f

    /** Taps within this distance (in board units) of a point select it. */
    private const val TOUCH_RADIUS = SPACING * 0.45f

    fun scale(boardSizePx: Float): Float = boardSizePx / UNITS

    fun pointCenter(point: Int, boardSizePx: Float): Offset {
        val scale = scale(boardSizePx)
        return Offset(
            (FIRST_POINT + Board.col(point) * SPACING) * scale,
            (FIRST_POINT + Board.row(point) * SPACING) * scale,
        )
    }

    /** The point under [offset], or null if the tap is not close enough to any point. */
    fun pointAt(offset: Offset, boardSizePx: Float): Int? {
        val scale = scale(boardSizePx)
        if (scale <= 0f) return null
        val col = Math.round((offset.x / scale - FIRST_POINT) / SPACING)
        val row = Math.round((offset.y / scale - FIRST_POINT) / SPACING)
        if (row !in 0 until Board.SIZE || col !in 0 until Board.SIZE) return null
        val point = Board.index(row, col)
        val distance = (offset - pointCenter(point, boardSizePx)).getDistance() / scale
        return point.takeIf { distance <= TOUCH_RADIUS }
    }
}
