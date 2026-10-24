package com.matinstudios.baghchal.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.matinstudios.baghchal.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * GDPR/EEA consent through Google's UMP SDK. No ad is requested until [canRequestAds] is true;
 * outside regulated regions it becomes true without showing anything.
 */
class ConsentManager(private val context: Context) {
    private val info = UserMessagingPlatform.getConsentInformation(context)

    // Consent given in an earlier session counts straight away, so ads can start before the update returns.
    private val _canRequestAds = MutableStateFlow(info.canRequestAds())
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(isPrivacyOptionsRequired())

    /** Settings must offer a way to change consent (regulated regions only). */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    /** Call on every launch: refreshes consent status and shows the form if the player still has to answer. */
    fun gather(activity: Activity) {
        info.requestConsentInfoUpdate(
            activity,
            requestParameters(),
            { UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { refresh() } },
            { refresh() }, // Offline etc.: keep whatever consent we had.
        )
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { refresh() }
    }

    private fun refresh() {
        _canRequestAds.value = info.canRequestAds()
        _privacyOptionsRequired.value = isPrivacyOptionsRequired()
    }

    private fun isPrivacyOptionsRequired() =
        info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    private fun requestParameters(): ConsentRequestParameters {
        val builder = ConsentRequestParameters.Builder()
        if (BuildConfig.UMP_FORCE_EEA) {
            builder.setConsentDebugSettings(
                ConsentDebugSettings.Builder(context)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                    .addTestDeviceHashedId(BuildConfig.UMP_TEST_DEVICE_ID)
                    .build(),
            )
        }
        return builder.build()
    }
}
