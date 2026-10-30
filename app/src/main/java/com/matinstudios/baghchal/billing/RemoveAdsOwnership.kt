package com.matinstudios.baghchal.billing

/** Product ID of the one-time (lifetime) "Remove Ads" purchase in Play Console. */
const val REMOVE_ADS_PRODUCT_ID = "remove_ads"

/** Product ID of the one-year ad-free subscription in Play Console. */
const val YEARLY_PASS_PRODUCT_ID = "ad_free_year"

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
 * Ownership of one ad-free product ([REMOVE_ADS_PRODUCT_ID] by default, or [YEARLY_PASS_PRODUCT_ID]).
 *
 * @property owned the product has been paid for (a pending payment doesn't count yet).
 * @property toAcknowledge purchase tokens that must be acknowledged, or Play refunds them after 3 days.
 */
data class RemoveAdsOwnership(val owned: Boolean, val toAcknowledge: List<String>) {
    companion object {
        fun of(purchases: List<PurchaseRecord>, productId: String = REMOVE_ADS_PRODUCT_ID): RemoveAdsOwnership {
            val paid = purchases.filter { productId in it.products && it.state == PurchaseRecord.State.PURCHASED }
            return RemoveAdsOwnership(
                owned = paid.isNotEmpty(),
                toAcknowledge = paid.filterNot { it.acknowledged }.map { it.token },
            )
        }
    }
}
