package com.matinstudios.baghchal.data

import android.content.Context
import com.matinstudios.baghchal.CurrentActivity
import com.matinstudios.baghchal.ads.Ads
import com.matinstudios.baghchal.ads.ConsentManager
import com.matinstudios.baghchal.ads.MobileAdsManager
import com.matinstudios.baghchal.ads.NoAds
import com.matinstudios.baghchal.billing.BillingManager
import com.matinstudios.baghchal.online.BotFallbackOnline
import com.matinstudios.baghchal.online.FirebaseOnlineService
import com.matinstudios.baghchal.online.NoOnline
import com.matinstudios.baghchal.online.OnlineService
import com.google.firebase.FirebaseApp
import com.matinstudios.baghchal.ui.sound.MediaMusicPlayer
import com.matinstudios.baghchal.ui.sound.MusicPlayer
import com.matinstudios.baghchal.ui.sound.NoMusic
import com.matinstudios.baghchal.ui.sound.SoundPoolPlayer
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * App-wide singletons, created once in the Application.
 *
 * @param servicesEnabled false in instrumented tests: no consent form, ads, Play Billing, online play or music.
 */
class AppContainer(context: Context, val servicesEnabled: Boolean = true) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val dataStore = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("baghchal") },
    )

    val activities = CurrentActivity()
    val writer = SerialWriter(appScope)
    val savedGames = SavedGameRepository(dataStore)
    val stats = StatsRepository(dataStore)
    val settings = SettingsRepository(dataStore)
    val entitlements = EntitlementRepository(dataStore)

    /** The board to draw: the selected theme if it is unlocked, otherwise the free one. */
    val boardTheme: Flow<BoardTheme> = combine(settings.settings, entitlements.entitlements) { settings, owned ->
        owned.effectiveTheme(settings.boardTheme)
    }

    /** The pieces to draw: the selected set if it is unlocked, otherwise the free one. */
    val pieceSet: Flow<PieceSet> = combine(settings.settings, entitlements.entitlements) { settings, owned ->
        owned.effectivePieceSet(settings.pieceSet)
    }

    /** How the player is ad-free right now (Remove Ads, the yearly pass or a day pass). */
    val adFree: Flow<AdFree> = entitlements.entitlements.adFree()

    /** Which launcher icon the home screen shows. */
    val launcherIcons = LauncherIcons(context)

    val consent = ConsentManager(context)
    val ads: Ads = if (servicesEnabled) {
        MobileAdsManager(context, consent, adFree, activities, appScope)
    } else {
        NoAds
    }
    val billing = BillingManager(context, entitlements, activities, appScope)

    /**
     * Online play needs a Firebase config (app/google-services.json); without one it is switched off.
     * Quick match falls back to a computer opponent when nobody turns up within a few seconds.
     */
    val online: OnlineService = if (servicesEnabled && firebaseConfigured(context)) {
        BotFallbackOnline(FirebaseOnlineService(), appScope)
    } else {
        NoOnline
    }

    /** Created at app start so every sound is loaded before the first game. */
    val sounds = SoundPoolPlayer(context)

    /** Background music; which track plays is decided by [com.matinstudios.baghchal.ui.AppViewModel]. */
    val music: MusicPlayer = if (servicesEnabled) MediaMusicPlayer(context) else NoMusic
}

/** True when the build included a Firebase config, so the default Firebase app exists (or can be made). */
private fun firebaseConfigured(context: Context): Boolean =
    FirebaseApp.getApps(context).isNotEmpty() || FirebaseApp.initializeApp(context) != null
