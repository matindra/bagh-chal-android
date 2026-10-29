package com.matinstudios.baghchal.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.matinstudios.baghchal.MainActivity

/** The app icons a player can pick for the home screen. Each one is a launcher alias in the manifest. */
enum class AppIcon(val aliasSuffix: String) {
    CLASSIC("Classic"),
    FACE_OFF("FaceOff"),
    GLYPH("Glyph"),
    BOARD("Board"),
    NIGHT("Night"),
    MASK("Mask"),
    BADGE("Badge"),
}

/**
 * Switches the launcher icon by enabling one `<activity-alias>` (all point at [MainActivity]) and
 * disabling the rest. The manifest enables only [AppIcon.CLASSIC], so a fresh install shows that one.
 * The home screen can take a few seconds to show the new icon.
 */
class LauncherIcons(private val context: Context) {
    private val packageManager: PackageManager = context.packageManager

    fun current(): AppIcon = AppIcon.entries.firstOrNull(::isEnabled) ?: AppIcon.CLASSIC

    fun select(icon: AppIcon) {
        // Enable the new alias before disabling the old, so the app always has a launcher entry.
        setEnabled(icon, true)
        for (other in AppIcon.entries) {
            if (other != icon) setEnabled(other, false)
        }
    }

    private fun isEnabled(icon: AppIcon): Boolean =
        when (packageManager.getComponentEnabledSetting(component(icon))) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> icon == AppIcon.CLASSIC
            else -> false
        }

    private fun setEnabled(icon: AppIcon, enabled: Boolean) {
        val state = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        packageManager.setComponentEnabledSetting(component(icon), state, PackageManager.DONT_KILL_APP)
    }

    private fun component(icon: AppIcon) = ComponentName(context, "$ALIAS_PREFIX${icon.aliasSuffix}")

    private companion object {
        /** Aliases are named like the activity's package: com.matinstudios.baghchal.LauncherClassic. */
        val ALIAS_PREFIX = MainActivity::class.java.name.substringBeforeLast('.') + ".Launcher"
    }
}
