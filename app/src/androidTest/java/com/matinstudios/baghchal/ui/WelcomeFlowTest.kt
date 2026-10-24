package com.matinstudios.baghchal.ui

import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.matinstudios.baghchal.BaghchalApplication
import com.matinstudios.baghchal.MainActivity
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.ui.screens.HOW_TO_PLAY_STEPS
import com.matinstudios.baghchal.ui.screens.WELCOME_PAGE_COUNT
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** First launch: the welcome tutorial, then the home screen. */
@RunWith(AndroidJUnit4::class)
class WelcomeFlowTest {

    private val container get() = ApplicationProvider.getApplicationContext<BaghchalApplication>().container

    /** A fresh install: the tutorial hasn't been seen and there's no saved game. */
    private inner class FirstLaunch : ExternalResource() {
        override fun before() = runBlocking {
            container.savedGames.clear()
            container.settings.setTutorialSeen(false)
        }

        // Leave the device as the other UI tests expect it.
        override fun after() = runBlocking { container.settings.setTutorialSeen(true) }
    }

    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(FirstLaunch()).around(compose)

    private fun str(@StringRes id: Int, vararg args: Any) = compose.activity.getString(id, *args)

    private fun text(@StringRes id: Int, vararg args: Any): SemanticsNodeInteraction = compose.onNodeWithText(str(id, *args))

    private fun waitForText(@StringRes id: Int, vararg args: Any) {
        compose.waitUntil(10_000) { compose.onAllNodes(hasText(str(id, *args))).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun awaitTutorialSeen() {
        compose.waitUntil(5_000) { runBlocking { container.settings.tutorialSeen.first() } }
    }

    @Test
    fun nextWalksThroughEveryRuleThenOpensHome() {
        waitForText(R.string.welcome_title)
        compose.onNodeWithContentDescription(str(R.string.welcome_page_description, 1, WELCOME_PAGE_COUNT)).assertIsDisplayed()

        HOW_TO_PLAY_STEPS.forEachIndexed { index, step ->
            text(R.string.welcome_next).performClick()
            waitForText(R.string.welcome_step_count, index + 1, HOW_TO_PLAY_STEPS.size)
            text(step.title).assertIsDisplayed()
            compose.onNodeWithContentDescription(str(step.illustrationDescription)).assertIsDisplayed()
        }

        text(R.string.welcome_next).assertDoesNotExist()
        text(R.string.welcome_start).performClick()
        waitForText(R.string.mode_vs_ai)
        text(R.string.mode_vs_player).assertIsDisplayed()
        awaitTutorialSeen()
    }

    @Test
    fun skipOpensHomeAndRemembersTheTutorialWasSeen() {
        waitForText(R.string.welcome_title)
        text(R.string.welcome_skip).performClick()
        waitForText(R.string.mode_vs_ai)
        text(R.string.welcome_title).assertDoesNotExist()
        awaitTutorialSeen()
    }
}
