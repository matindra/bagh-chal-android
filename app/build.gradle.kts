import com.android.build.api.dsl.ApplicationBuildType
import java.io.StringReader
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

/** Untracked per-machine settings; real AdMob IDs live here, never in source. */
val localProperties = Properties().apply {
    providers.fileContents(rootProject.layout.projectDirectory.file("local.properties")).asText.orNull
        ?.let { load(StringReader(it)) }
}

/**
 * AdMob IDs by local.properties key (also the BuildConfig field name), with Google's sample IDs.
 * Debug builds always use the samples; release uses the real IDs from local.properties and falls
 * back to the samples for any that are missing.
 */
val adIdKeys = mapOf(
    "ADMOB_APP_ID" to "ca-app-pub-3940256099942544~3347511713",
    "ADMOB_BANNER_HOME_ID" to "ca-app-pub-3940256099942544/9214589741",
    "ADMOB_BANNER_GAME_ID" to "ca-app-pub-3940256099942544/9214589741",
    "ADMOB_INTERSTITIAL_GAMEEND_ID" to "ca-app-pub-3940256099942544/1033173712",
    "ADMOB_REWARDED_UNDO_ID" to "ca-app-pub-3940256099942544/5224354917",
    "ADMOB_REWARDED_HINT_ID" to "ca-app-pub-3940256099942544/5224354917",
    "ADMOB_REWARDED_THEME_ID" to "ca-app-pub-3940256099942544/5224354917",
)

fun ApplicationBuildType.adIds(useRealIds: Boolean) {
    val ids = adIdKeys.mapValues { (key, testId) ->
        if (useRealIds) localProperties.getProperty(key)?.trim()?.ifEmpty { null } ?: testId else testId
    }
    manifestPlaceholders["admobAppId"] = ids.getValue("ADMOB_APP_ID")
    for ((key, id) in ids - "ADMOB_APP_ID") buildConfigField("String", key, "\"$id\"")
    // Debug only: `ump.forceEea=true` plus `ump.testDeviceId=<id from logcat>` in local.properties
    // show the consent form as if in the EEA.
    val forceEea = !useRealIds && localProperties.getProperty("ump.forceEea").toBoolean()
    val testDeviceId = if (useRealIds) "" else localProperties.getProperty("ump.testDeviceId").orEmpty()
    buildConfigField("boolean", "UMP_FORCE_EEA", forceEea.toString())
    buildConfigField("String", "UMP_TEST_DEVICE_ID", "\"$testDeviceId\"")
}

android {
    namespace = "com.matinstudios.baghchal"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.matinstudios.baghchal"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.matinstudios.baghchal.BaghchalTestRunner"
    }

    buildTypes {
        debug {
            adIds(useRealIds = false)
        }
        release {
            adIds(useRealIds = true)
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.all { it.useJUnitPlatform() }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.play.services.ads)
    implementation(libs.ump)
    implementation(libs.billing.ktx)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}