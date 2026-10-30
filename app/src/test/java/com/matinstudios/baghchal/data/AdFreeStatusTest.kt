package com.matinstudios.baghchal.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AdFreeStatusTest {

    @Test
    fun `a purchase shows at once and unrelated changes are not repeated`() = runTest {
        val owned = MutableStateFlow(Entitlements())
        val seen = mutableListOf<AdFree>()
        val job = launch { owned.adFree().collect { seen += it } }
        runCurrent()
        owned.value = owned.value.copy(unlockedThemes = setOf(BoardTheme.STONE))
        runCurrent()
        owned.value = owned.value.copy(yearlyPass = true)
        runCurrent()
        assertEquals(listOf(AdFree.NONE, AdFree.YEARLY), seen)
        job.cancel()
    }
}
