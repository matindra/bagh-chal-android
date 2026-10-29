package com.matinstudios.baghchal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.matinstudios.baghchal.BaghchalApplication
import com.matinstudios.baghchal.CurrentActivity
import com.matinstudios.baghchal.ads.ConsentManager
import com.matinstudios.baghchal.billing.BillingManager
import com.matinstudios.baghchal.billing.PurchaseLaunch
import com.matinstudios.baghchal.billing.RestoreResult
import com.matinstudios.baghchal.data.EntitlementRepository
import com.matinstudios.baghchal.data.Entitlements
import com.matinstudios.baghchal.data.SerialWriter
import com.matinstudios.baghchal.data.Settings
import com.matinstudios.baghchal.data.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * @property removeAdsPrice Play's localised price, once known.
 * @property purchasePending a Remove Ads payment is still being processed.
 */
data class SettingsUiState(
    val settings: Settings,
    val entitlements: Entitlements,
    val removeAdsPrice: String?,
    val purchasePending: Boolean,
    val privacyOptionsRequired: Boolean,
)

/** One-line feedback shown in Settings after a purchase action. */
enum class SettingsMessage { PURCHASE_UNAVAILABLE, RESTORED, NOT_OWNED, RESTORE_FAILED }

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val entitlements: EntitlementRepository,
    private val writer: SerialWriter,
    private val billing: BillingManager,
    private val consent: ConsentManager,
    private val activities: CurrentActivity,
) : ViewModel() {

    /** Null until loaded. */
    val uiState: StateFlow<SettingsUiState?> = combine(
        repository.settings,
        entitlements.entitlements,
        billing.price,
        billing.purchasePending,
        consent.privacyOptionsRequired,
    ) { settings, owned, price, pending, privacyOptions ->
        SettingsUiState(settings, owned, price, pending, privacyOptions)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    private val _message = MutableStateFlow<SettingsMessage?>(null)
    val message: StateFlow<SettingsMessage?> = _message.asStateFlow()

    private var actionJob: Job? = null

    fun onSoundChanged(enabled: Boolean) {
        writer.submit { repository.setSoundEnabled(enabled) }
    }

    fun onMusicChanged(enabled: Boolean) {
        writer.submit { repository.setMusicEnabled(enabled) }
    }

    fun onHapticsChanged(enabled: Boolean) {
        writer.submit { repository.setHapticsEnabled(enabled) }
    }

    fun onRemoveAds() {
        _message.value = null
        if (billing.launchPurchase() == PurchaseLaunch.UNAVAILABLE) _message.value = SettingsMessage.PURCHASE_UNAVAILABLE
    }

    fun onRestorePurchase() = runAction {
        _message.value = when (billing.restore()) {
            RestoreResult.RESTORED -> SettingsMessage.RESTORED
            RestoreResult.NOT_OWNED -> SettingsMessage.NOT_OWNED
            RestoreResult.FAILED -> SettingsMessage.RESTORE_FAILED
        }
    }

    fun onPrivacyOptions() {
        activities.activity?.let(consent::showPrivacyOptions)
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
                SettingsViewModel(
                    container.settings,
                    container.entitlements,
                    container.writer,
                    container.billing,
                    container.consent,
                    container.activities,
                )
            }
        }
    }
}
