package com.matinstudios.baghchal.billing

import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.matinstudios.baghchal.CurrentActivity
import com.matinstudios.baghchal.data.EntitlementRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class RestoreResult { RESTORED, NOT_OWNED, FAILED }

enum class PurchaseLaunch { STARTED, UNAVAILABLE }

/** What can be bought to go ad-free: lifetime Remove Ads (one-time) or a yearly subscription. */
enum class AdFreeProduct(val productId: String, val type: String) {
    LIFETIME(REMOVE_ADS_PRODUCT_ID, ProductType.INAPP),
    YEARLY(YEARLY_PASS_PRODUCT_ID, ProductType.SUBS),
}

/**
 * The ad-free purchases through Play Billing. Ownership is cached in [EntitlementRepository], so
 * the app knows it offline; it is re-checked on every launch ([connect]) and when the player taps
 * Restore Purchase. Play only lists a subscription while it is paid up, so a lapsed or cancelled
 * year is dropped at the next check.
 */
class BillingManager(
    context: Context,
    private val entitlements: EntitlementRepository,
    private val activities: CurrentActivity,
    private val scope: CoroutineScope,
) : PurchasesUpdatedListener {

    private val client = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().enablePrepaidPlans().build())
        .enableAutoServiceReconnection()
        .build()

    private val productDetails = mutableMapOf<AdFreeProduct, ProductDetails>()

    private val _prices = MutableStateFlow<Map<AdFreeProduct, String>>(emptyMap())

    /** Localised prices from Play, once known. */
    val prices: StateFlow<Map<AdFreeProduct, String>> = _prices.asStateFlow()

    private val _pending = MutableStateFlow<Set<AdFreeProduct>>(emptySet())

    /** Purchases awaiting payment (e.g. cash at a shop); they apply once the payment completes. */
    val pending: StateFlow<Set<AdFreeProduct>> = _pending.asStateFlow()

    /** Call once at app start; later calls reconnect automatically when needed. */
    fun connect() {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingResponseCode.OK) scope.launch { restore() }
            }

            // Auto reconnection re-establishes the service before the next call.
            override fun onBillingServiceDisconnected() = Unit
        })
    }

    /** Re-reads the player's purchases from Play and updates the cached entitlements. */
    suspend fun restore(): RestoreResult {
        loadProductDetails()
        var owned = false
        var failed = false
        for (product in AdFreeProduct.entries) {
            val result = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(product.type).build())
            if (result.billingResult.responseCode == BillingResponseCode.OK) {
                owned = handle(result.purchasesList, complete = setOf(product)) || owned
            } else {
                failed = true
            }
        }
        return when {
            owned -> RestoreResult.RESTORED
            failed -> RestoreResult.FAILED
            else -> RestoreResult.NOT_OWNED
        }
    }

    /** Opens Play's purchase sheet for [product] over the current activity. */
    fun launchPurchase(product: AdFreeProduct): PurchaseLaunch {
        val details = productDetails[product] ?: run {
            scope.launch { loadProductDetails() }
            return PurchaseLaunch.UNAVAILABLE
        }
        val activity = activities.activity ?: return PurchaseLaunch.UNAVAILABLE
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details)
        if (product.type == ProductType.SUBS) {
            productParams.setOfferToken(details.yearlyOffer()?.offerToken ?: return PurchaseLaunch.UNAVAILABLE)
        }
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(productParams.build())).build()
        val result = client.launchBillingFlow(activity, params)
        return if (result.responseCode == BillingResponseCode.OK) PurchaseLaunch.STARTED else PurchaseLaunch.UNAVAILABLE
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingResponseCode.OK -> if (purchases != null) scope.launch { handle(purchases, complete = emptySet()) }
            // Bought on another device, or the update was missed: ask Play for the full list.
            BillingResponseCode.ITEM_ALREADY_OWNED -> scope.launch { restore() }
            else -> Unit // Cancelled or failed: nothing changes.
        }
    }

    /**
     * Applies [purchases] to the cached entitlements and acknowledges new ones. Only for the
     * products in [complete] (a full list from [restore]) may a missing purchase revoke the
     * entitlement (a refund, or a year that ran out). True if any product is owned.
     */
    private suspend fun handle(purchases: List<Purchase>, complete: Set<AdFreeProduct>): Boolean {
        val records = purchases.map { it.toRecord() }
        var anyOwned = false
        for (product in AdFreeProduct.entries) {
            val ownership = RemoveAdsOwnership.of(records, product.productId)
            val listed = product in complete || records.any { product.productId in it.products }
            if (listed) {
                val pending = records.any { product.productId in it.products && it.state == PurchaseRecord.State.PENDING }
                _pending.value = if (pending) _pending.value + product else _pending.value - product
            }
            if (ownership.owned || product in complete) {
                when (product) {
                    AdFreeProduct.LIFETIME -> entitlements.setRemoveAds(ownership.owned)
                    AdFreeProduct.YEARLY -> entitlements.setYearlyPass(ownership.owned)
                }
            }
            for (token in ownership.toAcknowledge) {
                client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(token).build())
            }
            anyOwned = anyOwned || ownership.owned
        }
        return anyOwned
    }

    private suspend fun loadProductDetails() {
        for (product in AdFreeProduct.entries) {
            if (product in productDetails) continue
            val query = QueryProductDetailsParams.Product.newBuilder()
                .setProductId(product.productId)
                .setProductType(product.type)
                .build()
            val result = client.queryProductDetails(QueryProductDetailsParams.newBuilder().setProductList(listOf(query)).build())
            val details = result.productDetailsList?.firstOrNull { it.productId == product.productId } ?: continue
            productDetails[product] = details
            val price = when (product.type) {
                ProductType.SUBS -> details.yearlyOffer()?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
                else -> details.oneTimePurchaseOfferDetails?.formattedPrice
            } ?: continue
            _prices.value = _prices.value + (product to price)
        }
    }

    /** The subscription's base plan (an offer without its own ID), or its first offer. */
    private fun ProductDetails.yearlyOffer(): ProductDetails.SubscriptionOfferDetails? =
        subscriptionOfferDetails?.let { offers -> offers.firstOrNull { it.offerId == null } ?: offers.firstOrNull() }

    private fun Purchase.toRecord() = PurchaseRecord(
        products = products,
        state = when (purchaseState) {
            Purchase.PurchaseState.PURCHASED -> PurchaseRecord.State.PURCHASED
            Purchase.PurchaseState.PENDING -> PurchaseRecord.State.PENDING
            else -> PurchaseRecord.State.OTHER
        },
        acknowledged = isAcknowledged,
        token = purchaseToken,
    )
}
