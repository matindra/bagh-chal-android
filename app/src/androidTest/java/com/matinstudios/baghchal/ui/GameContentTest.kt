package com.matinstudios.baghchal.ui

import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.data.BoardTheme
import com.matinstudios.baghchal.engine.GameState
import com.matinstudios.baghchal.engine.Move
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.ui.board.boardPointTestTag
import com.matinstudios.baghchal.ui.screens.GameContent
import com.matinstudios.baghchal.ui.screens.GameInteraction
import com.matinstudios.baghchal.ui.screens.GameUiState
import com.matinstudios.baghchal.ui.theme.BaghchalTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The game screen rendered from hand-built states, without view models or storage. */
@RunWith(AndroidJUnit4::class)
class GameContentTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun str(@StringRes id: Int, vararg args: Any) = context.getString(id, *args)

    private val tapped = mutableListOf<Int>()
    private var newGameClicks = 0
    private var menuClicks = 0

    private fun show(ui: GameUiState) {
        compose.setContent {
            BaghchalTheme {
                GameContent(
                    uiState = ui,
                    boardTheme = BoardTheme.WOOD,
                    onPointTapped = { tapped += it },
                    onNewGame = { newGameClicks++ },
                    onMenu = { menuClicks++ },
                )
            }
        }
    }

    private fun point(index: Int) = compose.onNodeWithTag(boardPointTestTag(index))

    private fun description(row: Int, col: Int, @StringRes piece: Int) =
        str(R.string.point_description, row, col, str(piece))

    private val isPossibleMove = SemanticsMatcher.expectValue(
        SemanticsProperties.StateDescription,
        InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.point_possible_move),
    )

    // --- Accessibility of the board ---

    @Test
    fun everyPointIsAnAccessibleNodeDescribingItsContents() {
        show(GameInteraction.newGame())
        for (index in 0 until 25) {
            val piece = if (index in listOf(0, 4, 20, 24)) R.string.point_tiger else R.string.point_empty
            point(index).assertContentDescriptionEquals(description(index / 5 + 1, index % 5 + 1, piece))
        }
    }

    @Test
    fun describesGoatsSelectionAndPossibleMoves() {
        val placed = GameInteraction.onPointTapped(GameInteraction.newGame(), 7) // goat on row 2, column 3
        val tigerSelected = GameInteraction.onPointTapped(placed, 0)
        show(tigerSelected)

        point(7).assertContentDescriptionEquals(description(2, 3, R.string.point_goat))
        point(0).assertIsSelected()
        point(4).assertIsNotSelected()
        for (target in listOf(1, 5, 6)) point(target).assert(isPossibleMove)
        point(12).assert(isPossibleMove.not())
    }

    @Test
    fun accessibilityClickPlaysThatPoint() {
        show(GameInteraction.newGame())
        point(12).performSemanticsAction(SemanticsActions.OnClick)
        point(3).performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(listOf(12, 3), tapped)
    }

    @Test
    fun touchingAPointStillGoesThroughTheBoard() {
        show(GameInteraction.newGame())
        for (index in listOf(0, 12, 24, 7)) point(index).performClick()
        assertEquals(listOf(0, 12, 24, 7), tapped)
    }

    // --- Status line ---

    @Test
    fun showsTurnPhaseAndCounters() {
        show(GameInteraction.newGame())
        compose.onNodeWithText(str(R.string.turn_goats)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.phase_placement)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.goats_in_hand, 20)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.goats_captured, 0, 5)).assertIsDisplayed()
    }

    @Test
    fun showsThinkingIndicatorOnlyWhileTheComputerThinks() {
        val aiTurn = GameInteraction.newGame(aiSide = Side.GOAT)
        show(aiTurn.copy(isAiThinking = true))
        compose.onNodeWithText(str(R.string.ai_thinking)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.phase_placement)).assertDoesNotExist()
    }

    @Test
    fun noThinkingIndicatorOnThePlayersTurn() {
        show(GameInteraction.newGame(aiSide = Side.TIGER))
        compose.onNodeWithText(str(R.string.ai_thinking)).assertDoesNotExist()
        compose.onNodeWithText(str(R.string.phase_placement)).assertIsDisplayed()
    }

    // --- Game over ---

    /** Goat 7 -> 2 traps every tiger. */
    private fun goatsWin(aiSide: Side?): GameUiState {
        val game = GameState.create(
            tigers = listOf(0, 4, 20, 24),
            goats = listOf(1, 3, 5, 6, 7, 8, 9, 10, 12, 14, 15, 16, 18, 19, 21, 22, 23),
            sideToMove = Side.GOAT,
            goatsInHand = 0,
            goatsCaptured = 3,
        )
        return GameInteraction.applyMove(GameInteraction.fromGame(game, aiSide), Move.Step(7, 2))
    }

    private fun drawn(): GameUiState = GameInteraction.fromGame(
        GameState.create(listOf(0, 4, 20, 24), listOf(12), Side.GOAT, goatsInHand = 19, pliesSinceCapture = 100),
        aiSide = null,
    )

    @Test
    fun aDrawShowsResultAndButtonsThatWork() {
        show(drawn())
        compose.onNodeWithText(str(R.string.result_draw_move_limit)).assertIsDisplayed()

        compose.onNodeWithText(str(R.string.new_game)).performClick()
        compose.onNodeWithText(str(R.string.menu)).performClick()
        assertEquals(1, newGameClicks)
        assertEquals(1, menuClicks)
    }

    @Test
    fun aWinShowsTheResultButLeavesTheButtonsToTheResultScreen() {
        show(goatsWin(aiSide = Side.TIGER))
        compose.onNodeWithText(str(R.string.result_goats_win_trapped)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.result_you_win)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.new_game)).assertDoesNotExist()
        compose.onNodeWithText(str(R.string.menu)).assertDoesNotExist()
    }

    @Test
    fun trappedTigersAreAnnounced() {
        show(goatsWin(aiSide = null))
        for (corner in listOf(0, 4, 20, 24)) {
            point(corner).assertContentDescriptionEquals(
                description(corner / 5 + 1, corner % 5 + 1, R.string.point_tiger_trapped),
            )
        }
    }

    @Test
    fun losingToTheComputerSaysSo() {
        show(goatsWin(aiSide = Side.GOAT))
        compose.onNodeWithText(str(R.string.result_you_lose)).assertIsDisplayed()
    }

    @Test
    fun twoPlayerGameOverJustSaysGameOver() {
        show(goatsWin(aiSide = null))
        compose.onNodeWithText(str(R.string.game_over)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.result_you_win)).assertDoesNotExist()
    }

    @Test
    fun noGameOverButtonsWhileInProgress() {
        show(GameInteraction.newGame())
        compose.onNodeWithText(str(R.string.new_game)).assertDoesNotExist()
        compose.onNodeWithText(str(R.string.menu)).assertDoesNotExist()
    }

    @Test
    fun boardTapsAreIgnoredByTheScreenOnceTheGameIsOver() {
        // GameContent forwards taps; GameInteraction ignores them after the game ends.
        val over = goatsWin(aiSide = null)
        show(over)
        point(11).performClick()
        assertEquals(listOf(11), tapped)
        assertEquals(over, GameInteraction.onPointTapped(over, 11))
    }
}
