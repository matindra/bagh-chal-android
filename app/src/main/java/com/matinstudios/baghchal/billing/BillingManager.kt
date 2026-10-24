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

/**
 * The one-time Remove Ads purchase through Play Billing. Ownership is cached in
 * [EntitlementRepository], so the app knows it offline; it is re-checked on every launch
 * ([connect]) and when the player taps Restore Purchase.
 */
class BillingManager(
    context: Context,
    private val entitlements: EntitlementRepository,
    private val activities: CurrentActivity,
    private val scope: CoroutineScope,
) : PurchasesUpdatedListener {

    private val client = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    private var productDetails: ProductDetails? = null

    private val _price = MutableStateFlow<String?>(null)

    /** Localised price from Play, once known. */
    val price: StateFlow<String?> = _price.asStateFlow()

    private val _purchasePending = MutableStateFlow(false)

    /** A purchase is awaiting payment (e.g. cash at a shop); Remove Ads applies once it completes. */
    val purchasePending: StateFlow<Boolean> = _purchasePending.asStateFlow()

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

    /** Re-reads the player's purchases from Play and updates the cached entitlement. */
    suspend fun restore(): RestoreResult {
        loadProductDetails()
        val result = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build())
        if (result.billingResult.responseCode != BillingResponseCode.OK) return RestoreResult.FAILED
        val owned = handle(result.purchasesList)
        return if (owned) RestoreResult.RESTORED else RestoreResult.NOT_OWNED
    }

    /** Opens Play's purchase sheet over the current activity. */
    fun launchPurchase(): PurchaseLaunch {
        val details = productDetails ?: run {
            scope.launch { loadProductDetails() }
            return PurchaseLaunch.UNAVAILABLE
        }
        val activity = activities.activity ?: return PurchaseLaunch.UNAVAILABLE
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build()),
            )
            .build()
        val result = client.launchBillingFlow(activity, params)
        return if (result.responseCode == BillingResponseCode.OK) PurchaseLaunch.STARTED else PurchaseLaunch.UNAVAILABLE
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingResponseCode.OK -> if (purchases != null) scope.launch { handle(purchases, revokeIfMissing = false) }
            // Bought on another device, or the update was missed: ask Play for the full list.
            BillingResponseCode.ITEM_ALREADY_OWNED -> scope.launch { restore() }
            else -> Unit // Cancelled or failed: nothing changes.
        }
    }

    /**
     * Applies [purchases] to the cached entitlement and acknowledges new ones. Only a complete
     * list from [restore] may revoke Remove Ads (e.g. after a refund).
     */
    private suspend fun handle(purchases: List<Purchase>, revokeIfMissing: Boolean = true): Boolean {
        val records = purchases.map { it.toRecord() }
        val ownership = RemoveAdsOwnership.of(records)
        _purchasePending.value = records.any { REMOVE_ADS_PRODUCT_ID in it.products && it.state == PurchaseRecord.State.PENDING }
        if (ownership.owned || revokeIfMissing) entitlements.setRemoveAds(ownership.owned)
        for (token in ownership.toAcknowledge) {
            client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(token).build())
        }
        return ownership.owned
    }

    private suspend fun loadProductDetails() {
        if (productDetails != null) return
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(REMOVE_ADS_PRODUCT_ID)
            .setProductType(ProductType.INAPP)
            .build()
        val result = client.queryProductDetails(QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build())
        val details = result.productDetailsList?.firstOrNull { it.productId == REMOVE_ADS_PRODUCT_ID } ?: return
        productDetails = details
        _price.value = details.oneTimePurchaseOfferDetails?.formattedPrice
    }

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
