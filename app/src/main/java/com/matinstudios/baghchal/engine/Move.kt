package com.matinstudios.baghchal.engine

sealed interface Move {
    val to: Int

    /** Goat placed from hand (placement phase only). */
    data class Place(override val to: Int) : Move

    /** One step along a line to an adjacent empty point (goat or tiger). */
    data class Step(val from: Int, override val to: Int) : Move

    /** Tiger capture: jump over the goat on [over] to the empty point [to]. */
    data class Jump(val from: Int, val over: Int, override val to: Int) : Move
}
