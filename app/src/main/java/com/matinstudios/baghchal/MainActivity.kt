package com.matinstudios.baghchal

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.matinstudios.baghchal.ui.BaghchalApp
import com.matinstudios.baghchal.ui.theme.BaghchalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockPhonesToPortrait()
        enableEdgeToEdge()
        setContent {
            BaghchalTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BaghchalApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        (application as BaghchalApplication).container.sounds.setInForeground(true)
    }

    override fun onStop() {
        (application as BaghchalApplication).container.sounds.setInForeground(false)
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
