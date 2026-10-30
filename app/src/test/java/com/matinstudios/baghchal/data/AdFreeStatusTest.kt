package com.matinstudios.baghchal.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AdFreeStatusTest {

    @Test
    fun `ads come back the moment a day pass runs out`() = runTest {
        val clock = { testScheduler.currentTime }
        val owned = MutableStateFlow(Entitlements(dayPassUntilMillis = DAY_PASS_MILLIS))
        val seen = mutableListOf<AdFree>()
        val job = launch { owned.adFree(clock).collect { seen += it } }
        runCurrent()
        assertEquals(listOf(AdFree.DAY_PASS), seen)

        advanceTimeBy(DAY_PASS_MILLIS - 1)
        assertEquals(listOf(AdFree.DAY_PASS), seen)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf(AdFree.DAY_PASS, AdFree.NONE), seen)
        job.cancel()
    }

    @Test
    fun `a purchase during a day pass shows at once and stays after the pass ends`() = runTest {
        val clock = { testScheduler.currentTime }
        val owned = MutableStateFlow(Entitlements(dayPassUntilMillis = 1_000L))
        val seen = mutableListOf<AdFree>()
        val job = launch { owned.adFree(clock).collect { seen += it } }
        runCurrent()
        owned.value = owned.value.copy(yearlyPass = true)
        runCurrent()
        advanceTimeBy(2_000L)
        runCurrent()
        assertEquals(listOf(AdFree.DAY_PASS, AdFree.YEARLY), seen)
        job.cancel()
    }
}
