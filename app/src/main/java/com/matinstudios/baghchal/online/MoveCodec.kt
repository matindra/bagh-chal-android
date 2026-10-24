package com.matinstudios.baghchal.online

import com.matinstudios.baghchal.engine.Board
import com.matinstudios.baghchal.engine.Move

/** Moves as short strings for the online room: "P12" (place), "S0,1" (step), "J0,1,2" (jump). */
object MoveCodec {

    fun encode(move: Move): String = when (move) {
        is Move.Place -> "P${move.to}"
        is Move.Step -> "S${move.from},${move.to}"
        is Move.Jump -> "J${move.from},${move.over},${move.to}"
    }

    /** Null for anything malformed; whether the move is legal is checked separately. */
    fun decode(text: String): Move? {
        if (text.isEmpty()) return null
        val points = text.substring(1).split(',').map { it.toIntOrNull() ?: return null }
        if (points.any { it !in 0 until Board.POINTS }) return null
        return when {
            text[0] == 'P' && points.size == 1 -> Move.Place(points[0])
            text[0] == 'S' && points.size == 2 -> Move.Step(points[0], points[1])
            text[0] == 'J' && points.size == 3 -> Move.Jump(points[0], points[1], points[2])
            else -> null
        }
    }
}
