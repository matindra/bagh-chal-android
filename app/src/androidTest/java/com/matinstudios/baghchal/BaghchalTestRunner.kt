package com.matinstudios.baghchal

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/** Runs instrumented tests against [TestBaghchalApplication]. */
class BaghchalTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application =
        super.newApplication(cl, TestBaghchalApplication::class.java.name, context)
}

/** The real app without consent, ads or billing, so tests are deterministic and never show an ad. */
class TestBaghchalApplication : BaghchalApplication() {
    override val servicesEnabled = false
}
