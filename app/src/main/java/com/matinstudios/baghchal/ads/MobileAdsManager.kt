package com.matinstudios.baghchal.ads

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.matinstudios.baghchal.BuildConfig
import com.matinstudios.baghchal.CurrentActivity
import com.matinstudios.baghchal.data.AdFree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Google Mobile Ads, initialised once per process after consent allows it. [adFree] (any pass or
 * Remove Ads) turns off banners and interstitials; see [offeredWhen] for rewarded ads. Interstitial and
 * rewarded ads are preloaded and reloaded after each use; load failures are ignored and simply
 * mean that no ad is shown.
 */
class MobileAdsManager(
    private val context: Context,
    consent: ConsentManager,
    adFree: Flow<AdFree>,
    private val activities: CurrentActivity,
    scope: CoroutineScope,
) : Ads {
    private val initialized = MutableStateFlow(false)
    private val adFreeNow = MutableStateFlow(AdFree.NONE)
    private var interstitial: InterstitialAd? = null
    private var loadingInterstitial = false
    private val rewarded = mutableMapOf<RewardedPlacement, RewardedAd>()
    private val loadingRewarded = mutableSetOf<RewardedPlacement>()
    private val rewardedLoaded = RewardedPlacement.entries.associateWith { MutableStateFlow(false) }
    private val mainHandler = Handler(Looper.getMainLooper())
    private val retry = Runnable { preload() }
    private val interstitialPolicy = InterstitialPolicy(startedAtMillis = SystemClock.elapsedRealtime())

    override val bannersEnabled: StateFlow<Boolean> = combine(initialized, adFreeNow) { ready, free -> ready && free == AdFree.NONE }
        .stateIn(scope, SharingStarted.Eagerly, false)

    private val rewardedReady = RewardedPlacement.entries.associateWith { placement ->
        combine(rewardedLoaded.getValue(placement), adFreeNow) { loaded, free -> loaded && placement.offeredWhen(free) }
            .stateIn(scope, SharingStarted.Eagerly, false)
    }

    override fun rewardedReady(placement: RewardedPlacement): StateFlow<Boolean> = rewardedReady.getValue(placement)

    init {
        scope.launch(Dispatchers.Main) {
            adFree.collect {
                adFreeNow.value = it
                preload()
            }
        }
        scope.launch {
            consent.canRequestAds.first { it }
            // Initialisation does disk and network work; Google recommends a background thread.
            MobileAds.initialize(context)
            withContext(Dispatchers.Main) {
                initialized.value = true
                preload()
            }
        }
    }

    override suspend fun showRewarded(placement: RewardedPlacement): RewardOutcome = withContext(Dispatchers.Main) {
        val ad = rewarded[placement]
        val activity = activities.activity
        if (ad == null || activity == null || !placement.offeredWhen(adFreeNow.value)) {
            preload()
            return@withContext RewardOutcome.UNAVAILABLE
        }
        rewarded.remove(placement)
        rewardedLoaded.getValue(placement).value = false
        val outcome = suspendCancellableCoroutine { continuation ->
            var earned = false
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    continuation.resume(if (earned) RewardOutcome.EARNED else RewardOutcome.DISMISSED)
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    continuation.resume(RewardOutcome.UNAVAILABLE)
                }
            }
            ad.show(activity) { earned = true }
        }
        preload()
        outcome
    }

    override fun onGameFinished() {
        if (!interstitialPolicy.onGameFinished(SystemClock.elapsedRealtime())) return
        val ad = interstitial
        val activity = activities.activity
        if (ad == null || activity == null || adFreeNow.value != AdFree.NONE) {
            preload()
            return
        }
        interstitial = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() = interstitialPolicy.onShown(SystemClock.elapsedRealtime())
            override fun onAdDismissedFullScreenContent() = preload()
            override fun onAdFailedToShowFullScreenContent(error: AdError) = preload()
        }
        ad.show(activity)
    }

    /** Offline or no fill: try again later rather than hammering the network. */
    private fun retryLater() {
        mainHandler.removeCallbacks(retry)
        mainHandler.postDelayed(retry, RETRY_DELAY_MILLIS)
    }

    /** Main thread only. */
    private fun preload() {
        if (!initialized.value) return
        if (interstitial == null && !loadingInterstitial && adFreeNow.value == AdFree.NONE) {
            loadingInterstitial = true
            InterstitialAd.load(
                context,
                BuildConfig.ADMOB_INTERSTITIAL_GAMEEND_ID,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        loadingInterstitial = false
                        interstitial = ad
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        loadingInterstitial = false
                        retryLater()
                    }
                },
            )
        }
        for (placement in RewardedPlacement.entries) {
            if (placement in rewarded || placement in loadingRewarded || !placement.offeredWhen(adFreeNow.value)) continue
            loadingRewarded += placement
            RewardedAd.load(
                context,
                placement.adUnitId,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        loadingRewarded -= placement
                        rewarded[placement] = ad
                        rewardedLoaded.getValue(placement).value = true
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        loadingRewarded -= placement
                        retryLater()
                    }
                },
            )
        }
    }

    private val RewardedPlacement.adUnitId: String
        get() = when (this) {
            RewardedPlacement.UNDO -> BuildConfig.ADMOB_REWARDED_UNDO_ID
            RewardedPlacement.HINT -> BuildConfig.ADMOB_REWARDED_HINT_ID
            RewardedPlacement.THEME -> BuildConfig.ADMOB_REWARDED_THEME_ID
        }

    private companion object {
        const val RETRY_DELAY_MILLIS = 60_000L
    }
}
