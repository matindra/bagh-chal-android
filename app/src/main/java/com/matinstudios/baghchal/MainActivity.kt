package com.matinstudios.baghchal

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matinstudios.baghchal.ui.BaghchalApp
import com.matinstudios.baghchal.ui.ThemeViewModel
import com.matinstudios.baghchal.ui.board.LocalPieceArt
import com.matinstudios.baghchal.ui.board.art
import com.matinstudios.baghchal.ui.theme.BaghchalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockPhonesToPortrait()
        // Every screen is a night scene, so the status and navigation bar icons are always light.
        val bars = SystemBarStyle.dark(Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        val container = (application as BaghchalApplication).container
        // Once per launch (not on rotation): no ad is requested until consent allows it.
        if (savedInstanceState == null && container.servicesEnabled) container.consent.gather(this)
        setContent {
            val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModel.Factory)
            // Blank (window background) for the moment it takes to read the saved pieces.
            val pieceSet = themeViewModel.pieceSet.collectAsStateWithLifecycle().value ?: return@setContent
            BaghchalTheme {
                CompositionLocalProvider(LocalPieceArt provides pieceSet.art) {
                    // Each screen applies the bar padding itself, so Home can draw behind the bars.
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        BaghchalApp(barPadding = innerPadding)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val container = (application as BaghchalApplication).container
        container.sounds.setInForeground(true)
        container.music.setInForeground(true)
    }

    override fun onStop() {
        val container = (application as BaghchalApplication).container
        container.sounds.setInForeground(false)
        container.music.setInForeground(false)
        super.onStop()
    }

    /** Phones are portrait-only; tablets (smallest width >= 600dp) may rotate. Re-checked when a foldable unfolds. */
    private fun lockPhonesToPortrait() {
        if (resources.configuration.smallestScreenWidthDp < TABLET_MIN_WIDTH_DP) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    private companion object {
        const val TABLET_MIN_WIDTH_DP = 600
    }
}
