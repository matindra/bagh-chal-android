package com.matinstudios.baghchal

import android.app.Application
import com.matinstudios.baghchal.data.AppContainer

class BaghchalApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
