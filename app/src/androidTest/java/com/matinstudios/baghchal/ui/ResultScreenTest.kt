package com.matinstudios.baghchal.ui

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.engine.ai.Difficulty
import com.matinstudios.baghchal.ui.screens.GameResult
import com.matinstudios.baghchal.ui.screens.GameSetup
import com.matinstudios.baghchal.ui.screens.ResultScreen
import com.matinstudios.baghchal.ui.theme.BaghchalTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResultScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun str(@StringRes id: Int) = context.getString(id)

    private var playAgain = 0
    private var home = 0

    private fun show(result: GameResult) {
        compose.setContent {
            BaghchalTheme {
                ResultScreen(result, onPlayAgain = { playAgain++ }, onHome = { home++ })
            }
        }
    }

    @Test
    fun tigersWin() {
        show(GameResult(Side.TIGER, 41, GameSetup.TWO_PLAYER))
        compose.onNodeWithText(str(R.string.result_title_tigers)).assertIsDisplayed()
        compose.onNodeWithContentDescription(str(R.string.cd_result_tigers)).assertIsDisplayed()
        compose.onNodeWithContentDescription(str(R.string.cd_result_goats)).assertDoesNotExist()
        compose.onNodeWithText(context.resources.getQuantityString(R.plurals.result_moves, 41, 41)).assertIsDisplayed()
    }

    @Test
    fun goatsWin() {
        show(GameResult(Side.GOAT, 87, GameSetup.vsComputer(Side.GOAT, Difficulty.HARD)))
        compose.onNodeWithText(str(R.string.result_title_goats)).assertIsDisplayed()
        compose.onNodeWithContentDescription(str(R.string.cd_result_goats)).assertIsDisplayed()
        compose.onNodeWithText("87", substring = true).assertIsDisplayed()
    }

    @Test
    fun singleMoveIsSingular() {
        show(GameResult(Side.TIGER, 1, GameSetup.TWO_PLAYER))
        compose.onNodeWithText(context.resources.getQuantityString(R.plurals.result_moves, 1, 1)).assertIsDisplayed()
    }

    @Test
    fun buttonsWork() {
        show(GameResult(Side.GOAT, 30, GameSetup.TWO_PLAYER))
        compose.onNodeWithText(str(R.string.play_again)).performClick()
        compose.onNodeWithText(str(R.string.home)).performClick()
        assertEquals(1, playAgain)
        assertEquals(1, home)
    }
}
