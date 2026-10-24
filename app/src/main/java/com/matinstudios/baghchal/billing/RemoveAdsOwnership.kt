package com.matinstudios.baghchal.billing

/** Product ID of the one-time "Remove Ads" purchase in Play Console. */
const val REMOVE_ADS_PRODUCT_ID = "remove_ads"

/** The parts of a Play Billing purchase that matter here, so the rules can be unit-tested. */
data class PurchaseRecord(
    val products: List<String>,
    val state: State,
    val acknowledged: Boolean,
    val token: String,
) {
    enum class State { PURCHASED, PENDING, OTHER }
}

/**
 * @property owned Remove Ads has been paid for (a pending payment doesn't count yet).
 * @property toAcknowledge purchase tokens that must be acknowledged, or Play refunds them after 3 days.
 */
data class RemoveAdsOwnership(val owned: Boolean, val toAcknowledge: List<String>) {
    companion object {
        fun of(purchases: List<PurchaseRecord>): RemoveAdsOwnership {
            val paid = purchases.filter { REMOVE_ADS_PRODUCT_ID in it.products && it.state == PurchaseRecord.State.PURCHASED }
            return RemoveAdsOwnership(
                owned = paid.isNotEmpty(),
                toAcknowledge = paid.filterNot { it.acknowledged }.map { it.token },
            )
        }
    }
}
