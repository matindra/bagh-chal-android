package com.matinstudios.baghchal.ui.board

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope

/** A piece being carried from point [from]; [position] is the finger, in board pixels. */
data class PieceDrag(val from: Int, val position: Offset)

/**
 * Drags that start on a piece ([hasPiece]): [onStart] once the finger has moved past touch slop,
 * [onMove] while it moves, then [onEnd] with the point under the finger (null if none) or
 * [onCancel]. Presses elsewhere are left alone, so a slightly shaky tap on an empty point is
 * still a tap.
 */
suspend fun PointerInputScope.detectPieceDrags(
    boardSizePx: () -> Float,
    hasPiece: (Int) -> Boolean,
    onStart: (point: Int, position: Offset) -> Unit,
    onMove: (Offset) -> Unit,
    onEnd: (droppedOn: Int?) -> Unit,
    onCancel: () -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val from = BoardGeometry.pointAt(down.position, boardSizePx())
        if (from == null || !hasPiece(from)) return@awaitEachGesture
        val start = awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
            ?: return@awaitEachGesture
        var position = start.position
        onStart(from, position)
        val finished = drag(start.id) { change ->
            change.consume()
            position = change.position
            onMove(position)
        }
        if (finished) onEnd(BoardGeometry.pointAt(position, boardSizePx())) else onCancel()
    }
}
