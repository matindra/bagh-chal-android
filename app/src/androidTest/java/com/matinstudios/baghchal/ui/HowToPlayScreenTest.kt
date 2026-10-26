package com.matinstudios.baghchal.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.data.BoardTheme
import com.matinstudios.baghchal.ui.screens.HOW_TO_PLAY_STEPS
import com.matinstudios.baghchal.ui.screens.HowToPlayContent
import com.matinstudios.baghchal.ui.theme.BaghchalTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HowToPlayScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun str(@StringRes id: Int, vararg args: Any) = context.getString(id, *args)

    private var backClicks = 0

    private fun show(theme: BoardTheme = BoardTheme.WOOD) {
        compose.setContent { BaghchalTheme { HowToPlayContent(theme, onBack = { backClicks++ }) } }
    }

    @Test
    fun showsTheGoalAndFiveNumberedStepsEachWithADescribedPicture() {
        show()
        compose.onNodeWithText(str(R.string.rules_goal)).assertIsDisplayed()
        HOW_TO_PLAY_STEPS.forEachIndexed { index, step ->
            compose.onNodeWithText(str(R.string.rules_step_title, index + 1, str(step.title))).performScrollTo().assertIsDisplayed()
            compose.onNodeWithContentDescription(str(step.illustrationDescription)).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(str(step.body)).performScrollTo().assertIsDisplayed()
        }
        compose.onNodeWithText(str(R.string.rules_draws)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun rendersWithEveryBoardTheme() {
        var theme by mutableStateOf(BoardTheme.WOOD)
        compose.setContent { BaghchalTheme { HowToPlayContent(theme, onBack = {}) } }
        for (each in BoardTheme.entries) {
            theme = each
            compose.waitForIdle()
            compose.onNodeWithContentDescription(str(R.string.rules_place_picture)).assertIsDisplayed()
        }
    }

    @Test
    fun backButtonWorks() {
        show()
        compose.onNodeWithContentDescription(str(R.string.back)).performClick()
        assertEquals(1, backClicks)
    }
}
