package com.matinstudios.baghchal

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * The resumed activity, for app-wide services that must show something over it (consent form,
 * full-screen ads, the purchase sheet). Weakly held so it never leaks.
 */
class CurrentActivity : Application.ActivityLifecycleCallbacks {
    private var resumed = WeakReference<Activity>(null)

    val activity: Activity? get() = resumed.get()

    override fun onActivityResumed(activity: Activity) {
        resumed = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (resumed.get() === activity) resumed = WeakReference(null)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
