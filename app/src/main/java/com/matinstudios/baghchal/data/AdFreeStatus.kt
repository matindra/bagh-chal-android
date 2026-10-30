package com.matinstudios.baghchal.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transformLatest

/**
 * How the player is ad-free, re-emitted the moment a day pass runs out (nothing else changes
 * with time). [clock] is wall-clock milliseconds.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun Flow<Entitlements>.adFree(clock: () -> Long = System::currentTimeMillis): Flow<AdFree> =
    transformLatest { owned ->
        while (true) {
            val now = clock()
            emit(owned.adFree(now))
            val left = owned.dayPassLeftMillis(now)
            if (left == 0L) break
            delay(left)
        }
    }.distinctUntilChanged()
