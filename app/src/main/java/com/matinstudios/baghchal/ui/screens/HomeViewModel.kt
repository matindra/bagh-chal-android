package com.matinstudios.baghchal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.matinstudios.baghchal.BaghchalApplication
import com.matinstudios.baghchal.ads.Ads
import com.matinstudios.baghchal.billing.BillingManager
import com.matinstudios.baghchal.billing.PurchaseLaunch
import com.matinstudios.baghchal.data.EntitlementRepository
import com.matinstudios.baghchal.data.SavedGame
import com.matinstudios.baghchal.data.SavedGameRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    savedGames: SavedGameRepository,
    ads: Ads,
    entitlements: EntitlementRepository,
    private val billing: BillingManager,
) : ViewModel() {

    /** The auto-saved game the player can resume, if any. */
    val savedGame: StateFlow<SavedGame?> = savedGames.savedGame
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    val showBanner: StateFlow<Boolean> = ads.bannersEnabled

    /** The Remove Ads button shows until the purchase is owned (hidden while that is still loading). */
    val showRemoveAds: StateFlow<Boolean> = entitlements.entitlements
        .map { !it.removeAds }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = false)

    private val _purchaseUnavailable = Channel<Unit>(Channel.CONFLATED)

    /** Emits when Play's purchase sheet couldn't be opened. */
    val purchaseUnavailable: Flow<Unit> = _purchaseUnavailable.receiveAsFlow()

    fun onRemoveAds() {
        if (billing.launchPurchase() == PurchaseLaunch.UNAVAILABLE) _purchaseUnavailable.trySend(Unit)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as BaghchalApplication).container
                HomeViewModel(container.savedGames, container.ads, container.entitlements, container.billing)
            }
        }
    }
}
