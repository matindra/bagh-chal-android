package com.matinstudios.baghchal.ui

import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.matinstudios.baghchal.BaghchalApplication
import com.matinstudios.baghchal.MainActivity
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.tutorial.TutorialScript
import com.matinstudios.baghchal.ui.board.boardPointTestTag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** First launch: the guided tutorial game, then the home screen. */
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

    private fun point(index: Int) = compose.onNodeWithTag(boardPointTestTag(index))

    private fun next() = text(R.string.welcome_next).performClick()

    @Test
    fun guidedGameWalksThroughEveryLessonThenOpensHome() {
        val steps = TutorialScript.STEPS.size
        waitForText(R.string.welcome_title)
        text(R.string.tutorial_step_count, 1, steps).assertIsDisplayed()
        next()

        // Place a goat: a wrong point is refused, the glowing one is taken, then the tiger answers.
        waitForText(R.string.tutorial_first_goat_instruction)
        point(6).performClick()
        waitForText(R.string.tutorial_nudge_glowing)
        point(12).performClick()
        waitForText(R.string.tutorial_first_goat_done)
        next()

        waitForText(R.string.tutorial_block_instruction)
        point(16).performClick()
        waitForText(R.string.tutorial_block_done)
        next()

        waitForText(R.string.tutorial_careless_instruction)
        point(7).performClick()
        waitForText(R.string.tutorial_careless_done)
        next()

        // A point the tiger could jump is refused with a warning; a safe one is fine.
        waitForText(R.string.tutorial_safe_instruction)
        point(6).performClick()
        waitForText(R.string.tutorial_nudge_danger)
        point(1).performClick()
        waitForText(R.string.tutorial_safe_done)
        next()

        waitForText(R.string.tutorial_trap_instruction)
        text(R.string.tutorial_step_count, steps, steps).assertIsDisplayed()
        point(17).performClick()
        point(18).performClick()
        waitForText(R.string.tutorial_trap_done)

        text(R.string.welcome_start).performClick()
        waitForText(R.string.mode_vs_ai)
        text(R.string.mode_vs_player).performScrollTo().assertIsDisplayed()
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
