package com.matinstudios.baghchal.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Runs storage writes one at a time, in submission order, on a long-lived [scope] — so a save
 * submitted just before the user leaves a screen still completes, and a later save can't be
 * overtaken by an earlier one.
 */
class SerialWriter(scope: CoroutineScope) {
    private val queue = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (write in queue) {
                try {
                    write()
                } catch (_: IOException) {
                    // Storage failures are non-fatal: the game keeps working, it just isn't saved.
                }
            }
        }
    }

    fun submit(write: suspend () -> Unit) {
        queue.trySend(write)
    }
}
