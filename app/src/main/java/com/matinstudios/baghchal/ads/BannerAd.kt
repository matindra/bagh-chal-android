package com.matinstudios.baghchal.ads

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.matinstudios.baghchal.BuildConfig

/** Each screen's banner has its own ad unit. */
enum class BannerPlacement { HOME, GAME }

/**
 * Anchored adaptive banner as wide as its container. Its height is reserved before the ad loads,
 * so nothing shifts under the player's finger when it arrives.
 */
@Composable
fun BannerAd(placement: BannerPlacement, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val context = LocalContext.current
        val widthDp = maxWidth.value.toInt()
        val adSize = remember(widthDp) { AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, widthDp) }
        val adView = remember(adSize, placement) {
            AdView(context).apply {
                setAdSize(adSize)
                adUnitId = when (placement) {
                    BannerPlacement.HOME -> BuildConfig.ADMOB_BANNER_HOME_ID
                    BannerPlacement.GAME -> BuildConfig.ADMOB_BANNER_GAME_ID
                }
                loadAd(AdRequest.Builder().build())
            }
        }
        DisposableEffect(adView) { onDispose { adView.destroy() } }
        LifecycleResumeEffect(adView) {
            adView.resume()
            onPauseOrDispose { adView.pause() }
        }
        AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth().height(adSize.height.dp))
    }
}
