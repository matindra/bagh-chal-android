package com.matinstudios.baghchal

import android.app.UiModeManager
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matinstudios.baghchal.data.ThemeMode
import com.matinstudios.baghchal.ui.BaghchalApp
import com.matinstudios.baghchal.ui.ThemeViewModel
import com.matinstudios.baghchal.ui.board.LocalPieceArt
import com.matinstudios.baghchal.ui.board.art
import com.matinstudios.baghchal.ui.theme.BaghchalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockPhonesToPortrait()
        enableEdgeToEdge()
        val container = (application as BaghchalApplication).container
        // Once per launch (not on rotation): no ad is requested until consent allows it.
        if (savedInstanceState == null && container.servicesEnabled) container.consent.gather(this)
        setContent {
            val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModel.Factory)
            // Blank (window background) for the moment it takes to read the saved theme.
            val darkTheme = isDarkTheme(themeViewModel) ?: return@setContent
            val pieceSet = themeViewModel.pieceSet.collectAsStateWithLifecycle().value ?: return@setContent
            val boardTheme = themeViewModel.boardTheme.collectAsStateWithLifecycle().value ?: return@setContent
            // Status and navigation bar icons follow the app's theme, not the phone's.
            DisposableEffect(darkTheme) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            BaghchalTheme(darkTheme = darkTheme, boardTheme = boardTheme) {
                CompositionLocalProvider(LocalPieceArt provides pieceSet.art) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        BaghchalApp(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }

    /** Null until the saved theme is read. */
    @Composable
    private fun isDarkTheme(themeViewModel: ThemeViewModel): Boolean? {
        val mode by themeViewModel.themeMode.collectAsStateWithLifecycle()
        val systemDark = isSystemInDarkTheme()
        val current = mode ?: return null
        LaunchedEffect(current) { applyToLaunchWindow(current) }
        return when (current) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> systemDark
        }
    }

    /**
     * Tells the system the app's night mode (Android 12+), so the launch window is drawn in the
     * chosen theme instead of flashing the phone's. The system remembers it across launches.
     * Older versions can't change the launch window without a library, so they keep the phone's.
     */
    private fun applyToLaunchWindow(mode: ThemeMode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val nightMode = when (mode) {
            ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
            ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
            ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO // follow the phone
        }
        getSystemService(UiModeManager::class.java).setApplicationNightMode(nightMode)
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
