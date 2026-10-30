package com.matinstudios.baghchal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.matinstudios.baghchal.BaghchalApplication
import com.matinstudios.baghchal.ads.Ads
import com.matinstudios.baghchal.ads.RewardOutcome
import com.matinstudios.baghchal.ads.RewardedPlacement
import com.matinstudios.baghchal.ads.grantsDayPass
import com.matinstudios.baghchal.billing.AdFreeProduct
import com.matinstudios.baghchal.billing.BillingManager
import com.matinstudios.baghchal.billing.PurchaseLaunch
import com.matinstudios.baghchal.billing.RestoreResult
import com.matinstudios.baghchal.data.AdFree
import com.matinstudios.baghchal.data.EntitlementRepository
import com.matinstudios.baghchal.data.SerialWriter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * @property dayPassUntilMillis when the day pass ends (only meaningful for [AdFree.DAY_PASS]).
 * @property prices Play's localised prices, once known.
 * @property pending purchases still waiting for payment.
 */
data class AdFreeUiState(
    val adFree: AdFree,
    val dayPassUntilMillis: Long,
    val prices: Map<AdFreeProduct, String>,
    val pending: Set<AdFreeProduct>,
)

/** One-line feedback after an action on the ad-free screen. */
enum class AdFreeMessage { DAY_PASS_STARTED, AD_SKIPPED, NO_AD, PURCHASE_UNAVAILABLE, RESTORED, NOT_OWNED, RESTORE_FAILED }

/** The three ways to go ad-free: a day for a rewarded ad, a yearly subscription, or for good. */
class AdFreeViewModel(
    adFree: Flow<AdFree>,
    private val entitlements: EntitlementRepository,
    private val writer: SerialWriter,
    private val ads: Ads,
    private val billing: BillingManager,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    /** Null until loaded. */
    val uiState: StateFlow<AdFreeUiState?> = combine(
        adFree,
        entitlements.entitlements,
        billing.prices,
        billing.pending,
    ) { free, owned, prices, pending ->
        AdFreeUiState(free, owned.dayPassUntilMillis, prices, pending)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    private val _message = MutableStateFlow<AdFreeMessage?>(null)
    val message: StateFlow<AdFreeMessage?> = _message.asStateFlow()

    private var actionJob: Job? = null

    /** Watching the whole ad starts 24 ad-free hours. */
    fun onWatchAd() = runAction {
        val outcome = ads.showRewarded(RewardedPlacement.AD_FREE_DAY)
        _message.value = when {
            outcome.grantsDayPass() -> {
                val now = clock()
                writer.submit { entitlements.startDayPass(now) }
                AdFreeMessage.DAY_PASS_STARTED
            }
            outcome == RewardOutcome.DISMISSED -> AdFreeMessage.AD_SKIPPED
            else -> AdFreeMessage.NO_AD
        }
    }

    fun onBuy(product: AdFreeProduct) {
        _message.value = null
        if (billing.launchPurchase(product) == PurchaseLaunch.UNAVAILABLE) _message.value = AdFreeMessage.PURCHASE_UNAVAILABLE
    }

    fun onRestorePurchase() = runAction {
        _message.value = when (billing.restore()) {
            RestoreResult.RESTORED -> AdFreeMessage.RESTORED
            RestoreResult.NOT_OWNED -> AdFreeMessage.NOT_OWNED
            RestoreResult.FAILED -> AdFreeMessage.RESTORE_FAILED
        }
    }

    private fun runAction(action: suspend () -> Unit) {
        if (actionJob?.isActive == true) return
        _message.value = null
        actionJob = viewModelScope.launch { action() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as BaghchalApplication).container
                AdFreeViewModel(
                    container.adFree,
                    container.entitlements,
                    container.writer,
                    container.ads,
                    container.billing,
                )
            }
        }
    }
}
