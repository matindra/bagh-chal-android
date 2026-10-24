package com.matinstudios.baghchal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.matinstudios.baghchal.BaghchalApplication
import com.matinstudios.baghchal.ads.Ads

class ResultViewModel(private val ads: Ads) : ViewModel() {

    /** Leaving the result is the only point between games, so it's where a (capped) interstitial may appear. */
    fun onLeave() = ads.onGameFinished()

    companion object {
        val Factory = viewModelFactory {
            initializer { ResultViewModel((this[APPLICATION_KEY] as BaghchalApplication).container.ads) }
        }
    }
}
