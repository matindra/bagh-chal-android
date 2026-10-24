package com.matinstudios.baghchal.billing

import com.matinstudios.baghchal.billing.PurchaseRecord.State
import kotlin.test.Test
import kotlin.test.assertEquals

class RemoveAdsOwnershipTest {

    private fun purchase(state: State, acknowledged: Boolean = true, product: String = REMOVE_ADS_PRODUCT_ID, token: String = "t") =
        PurchaseRecord(listOf(product), state, acknowledged, token)

    @Test
    fun `no purchases means ads stay`() {
        assertEquals(RemoveAdsOwnership(owned = false, toAcknowledge = emptyList()), RemoveAdsOwnership.of(emptyList()))
    }

    @Test
    fun `a completed purchase removes ads`() {
        assertEquals(RemoveAdsOwnership(owned = true, toAcknowledge = emptyList()), RemoveAdsOwnership.of(listOf(purchase(State.PURCHASED))))
    }

    @Test
    fun `a new purchase must be acknowledged`() {
        val ownership = RemoveAdsOwnership.of(listOf(purchase(State.PURCHASED, acknowledged = false, token = "new")))
        assertEquals(RemoveAdsOwnership(owned = true, toAcknowledge = listOf("new")), ownership)
    }

    @Test
    fun `a pending payment does not remove ads and is not acknowledged yet`() {
        val ownership = RemoveAdsOwnership.of(listOf(purchase(State.PENDING, acknowledged = false)))
        assertEquals(RemoveAdsOwnership(owned = false, toAcknowledge = emptyList()), ownership)
    }

    @Test
    fun `other products are ignored`() {
        val ownership = RemoveAdsOwnership.of(listOf(purchase(State.PURCHASED, acknowledged = false, product = "something_else")))
        assertEquals(RemoveAdsOwnership(owned = false, toAcknowledge = emptyList()), ownership)
    }
}
