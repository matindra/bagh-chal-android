package com.matinstudios.baghchal.engine

/** A capture line from a point: jump over [over] and land on [landing]. */
data class JumpLine(val over: Int, val landing: Int)

/**
 * The 5×5 Bagh Chal board. Points are indexed `row * 5 + col`.
 *
 * All points connect orthogonally to their neighbours; diagonal lines exist only through
 * points where `(row + col)` is even, so diagonal neighbours and jumps start from those points only.
 */
object Board {
    const val SIZE = 5
    const val POINTS = SIZE * SIZE

    val CORNERS: List<Int> = listOf(0, SIZE - 1, POINTS - SIZE, POINTS - 1)

    fun index(row: Int, col: Int): Int = row * SIZE + col
    fun row(point: Int): Int = point / SIZE
    fun col(point: Int): Int = point % SIZE

    fun hasDiagonals(point: Int): Boolean = (row(point) + col(point)) % 2 == 0

    private val ORTHOGONAL = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
    private val DIAGONAL = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)

    private fun directions(point: Int) =
        if (hasDiagonals(point)) ORTHOGONAL + DIAGONAL else ORTHOGONAL

    private fun onBoard(row: Int, col: Int) = row in 0 until SIZE && col in 0 until SIZE

    /** `adjacency[p]` = points one step from `p` along a line. */
    val adjacency: List<List<Int>> = List(POINTS) { p ->
        directions(p).mapNotNull { (dr, dc) ->
            val r = row(p) + dr
            val c = col(p) + dc
            if (onBoard(r, c)) index(r, c) else null
        }
    }

    /** `jumps[p]` = straight-line captures available from `p` (over an adjacent point to the one beyond). */
    val jumps: List<List<JumpLine>> = List(POINTS) { p ->
        directions(p).mapNotNull { (dr, dc) ->
            val r = row(p) + 2 * dr
            val c = col(p) + 2 * dc
            if (onBoard(r, c)) JumpLine(index(row(p) + dr, col(p) + dc), index(r, c)) else null
        }
    }

    fun isAdjacent(a: Int, b: Int): Boolean = b in adjacency[a]
}
