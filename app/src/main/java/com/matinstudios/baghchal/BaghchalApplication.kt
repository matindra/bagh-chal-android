package com.matinstudios.baghchal

import android.app.Application
import com.matinstudios.baghchal.data.AppContainer

open class BaghchalApplication : Application() {
    lateinit var container: AppContainer
        private set

    /** Consent, ads and billing; switched off by the instrumented-test application. */
    protected open val servicesEnabled: Boolean = true

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this, servicesEnabled)
        registerActivityLifecycleCallbacks(container.activities)
        // Restores Remove Ads on every launch; the cached entitlement covers offline starts.
        if (servicesEnabled) container.billing.connect()
    }
}
