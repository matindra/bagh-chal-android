package com.matinstudios.baghchal.ui.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import com.matinstudios.baghchal.engine.Move
import kotlin.math.PI
import kotlin.math.sin

/**
 * How the piece that just moved is drawn at animation [progress] 0..1: steps slide, jumps arc
 * over the captured goat (which fades out), placements pop in. Pure so it can be unit-tested.
 */
object PieceMotion {
    const val STEP_MILLIS = 250
    const val JUMP_MILLIS = 420
    const val PLACE_MILLIS = 200

    /** Peak height of the capture arc, in board units (lower near the top edge so the piece stays on the board). */
    const val JUMP_ARC_HEIGHT = 70f

    private const val JUMP_LIFT_SCALE = 0.2f
    private const val PLACE_START_SCALE = 0.6f

    fun durationMillis(move: Move): Int = when (move) {
        is Move.Place -> PLACE_MILLIS
        is Move.Step -> STEP_MILLIS
        is Move.Jump -> JUMP_MILLIS
    }

    /** Center of the moving piece. */
    fun center(move: Move, progress: Float, boardSizePx: Float): Offset {
        val to = BoardGeometry.pointCenter(move.to, boardSizePx)
        return when (move) {
            is Move.Place -> to
            is Move.Step -> lerp(BoardGeometry.pointCenter(move.from, boardSizePx), to, progress)
            is Move.Jump -> {
                val scale = BoardGeometry.scale(boardSizePx)
                val ground = lerp(BoardGeometry.pointCenter(move.from, boardSizePx), to, progress)
                val pieceRadius = BoardGeometry.PIECE_SIZE / 2 * scale(move, progress) * scale
                val lift = minOf(arc(progress) * JUMP_ARC_HEIGHT * scale, ground.y - pieceRadius)
                ground - Offset(0f, lift.coerceAtLeast(0f))
            }
        }
    }

    /** Size multiplier of the moving piece: a jumping tiger grows at the top of its arc. */
    fun scale(move: Move, progress: Float): Float = when (move) {
        is Move.Place -> PLACE_START_SCALE + (1f - PLACE_START_SCALE) * progress
        is Move.Step -> 1f
        is Move.Jump -> 1f + JUMP_LIFT_SCALE * arc(progress)
    }

    fun alpha(move: Move, progress: Float): Float = if (move is Move.Place) progress else 1f

    /** Opacity of the goat being captured. */
    fun capturedAlpha(progress: Float): Float = 1f - progress

    private fun arc(progress: Float): Float = sin(PI * progress).toFloat()
}
