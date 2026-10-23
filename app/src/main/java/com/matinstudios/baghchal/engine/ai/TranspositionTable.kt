package com.matinstudios.baghchal.engine.ai

import com.matinstudios.baghchal.engine.Move

/**
 * Fixed-size, always-replace transposition table keyed by the exact position key
 * (2 bits per point fits in a Long, so no Zobrist collisions are possible — only slot clashes).
 */
internal class TranspositionTable(sizeBits: Int = 17) {
    enum class Bound { EXACT, LOWER, UPPER }

    private val mask = (1 shl sizeBits) - 1
    private val keys = LongArray(1 shl sizeBits)
    private val depths = IntArray(1 shl sizeBits)
    private val scores = IntArray(1 shl sizeBits)
    private val bounds = ByteArray(1 shl sizeBits) // 0 = empty slot, else Bound.ordinal + 1
    private val moves = IntArray(1 shl sizeBits)

    private fun slot(key: Long): Int = ((key * -0x61c8864680b583ebL) ushr 40).toInt() and mask

    /** Slot index holding [key], or -1. */
    fun probe(key: Long): Int {
        val slot = slot(key)
        return if (bounds[slot].toInt() != 0 && keys[slot] == key) slot else -1
    }

    fun depth(slot: Int) = depths[slot]
    fun score(slot: Int) = scores[slot]
    fun bound(slot: Int) = Bound.entries[bounds[slot] - 1]
    fun move(slot: Int) = moves[slot]

    fun store(key: Long, depth: Int, score: Int, bound: Bound, move: Int) {
        val slot = slot(key)
        keys[slot] = key
        depths[slot] = depth
        scores[slot] = score
        bounds[slot] = (bound.ordinal + 1).toByte()
        moves[slot] = move
    }

    companion object {
        const val NO_MOVE = -1

        fun encode(move: Move): Int = when (move) {
            is Move.Place -> move.to
            is Move.Step -> (1 shl 15) or (move.from shl 10) or move.to
            is Move.Jump -> (2 shl 15) or (move.from shl 10) or (move.over shl 5) or move.to
        }
    }
}
