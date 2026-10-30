package com.matinstudios.baghchal.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** How the player is ad-free, re-emitted only when that changes. */
fun Flow<Entitlements>.adFree(): Flow<AdFree> = map { it.adFree() }.distinctUntilChanged()
