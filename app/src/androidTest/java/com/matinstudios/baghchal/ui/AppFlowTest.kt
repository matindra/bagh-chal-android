package com.matinstudios.baghchal.ui

import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.matinstudios.baghchal.BaghchalApplication
import com.matinstudios.baghchal.MainActivity
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.data.BoardTheme
import com.matinstudios.baghchal.data.SavedGame
import com.matinstudios.baghchal.engine.GameState
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.engine.ai.Difficulty
import com.matinstudios.baghchal.ui.board.boardPointTestTag
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** The real app: navigation, view models, storage and the AI. */
@RunWith(AndroidJUnit4::class)
class AppFlowTest {

    /** Starts every test from a clean slate: no saved game, no stats, default settings. */
    private class ResetAppData : ExternalResource() {
        override fun before() {
            val container = ApplicationProvider.getApplicationContext<BaghchalApplication>().container
            runBlocking {
                container.savedGames.clear()
                container.stats.reset()
                container.settings.setSoundEnabled(true)
                container.settings.setHapticsEnabled(true)
                container.settings.setBoardTheme(BoardTheme.WOOD)
            }
        }
    }

    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(ResetAppData()).around(compose)

    private fun str(@StringRes id: Int, vararg args: Any) = compose.activity.getString(id, *args)

    private fun text(@StringRes id: Int, vararg args: Any): SemanticsNodeInteraction =
        compose.onNodeWithText(str(id, *args))

    private fun point(index: Int) = compose.onNodeWithTag(boardPointTestTag(index))

    private companion object {
        const val FRAME_MILLIS = 16L
    }

    private fun waitForText(@StringRes id: Int, vararg args: Any, timeoutMillis: Long = 10_000) {
        compose.waitUntil(timeoutMillis) {
            compose.onAllNodes(hasText(str(id, *args))).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun startTwoPlayer() {
        text(R.string.home_two_player).performScrollTo().performClick()
        waitForText(R.string.turn_goats)
    }

    @Test
    fun homeToTwoPlayerGame() {
        text(R.string.home_vs_computer).assertIsDisplayed()
        startTwoPlayer()
        text(R.string.goats_in_hand, 20).assertIsDisplayed()
        point(0).assertContentDescriptionEquals(str(R.string.point_description, 1, 1, str(R.string.point_tiger)))
    }

    @Test
    fun homeToHowToPlayAndBack() {
        text(R.string.home_how_to_play).performScrollTo().performClick()
        waitForText(R.string.rules_title)
        text(R.string.rules_goal).assertIsDisplayed()
        Espresso.pressBack()
        waitForText(R.string.home_vs_computer)
    }

    @Test
    fun tapToMoveOnTheBoard() {
        startTwoPlayer()

        point(12).performClick() // goat placed in the centre
        waitForText(R.string.turn_tigers)
        text(R.string.goats_in_hand, 19).assertIsDisplayed()
        point(12).assertContentDescriptionEquals(str(R.string.point_description, 3, 3, str(R.string.point_goat)))

        point(0).performClick() // select the top-left tiger…
        point(1).performClick() // …and step right
        waitForText(R.string.turn_goats)
        point(0).assertContentDescriptionEquals(str(R.string.point_description, 1, 1, str(R.string.point_empty)))
        point(1).assertContentDescriptionEquals(str(R.string.point_description, 1, 2, str(R.string.point_tiger)))
    }

    @Test
    fun playingTigersTheComputerMovesFirstWhileShowingTheThinkingIndicator() {
        text(R.string.side_tigers).performClick()
        text(R.string.difficulty_hard).performClick()
        text(R.string.home_play).performScrollTo()
        // The spinner animates forever, so Compose is never idle while it shows: drive frames by hand.
        compose.mainClock.autoAdvance = false
        text(R.string.home_play).performClick()

        waitForTextWithManualClock(R.string.ai_thinking)
        text(R.string.turn_goats).assertIsDisplayed()

        // The computer's goat lands and it becomes the player's turn.
        waitForTextWithManualClock(R.string.turn_tigers)
        compose.mainClock.autoAdvance = true
        text(R.string.ai_thinking).assertDoesNotExist()
        text(R.string.goats_in_hand, 19).assertIsDisplayed()
    }

    private fun waitForTextWithManualClock(@StringRes id: Int, timeoutMillis: Long = 10_000) {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (SystemClock.uptimeMillis() < deadline) {
            compose.mainClock.advanceTimeByFrame()
            if (compose.onAllNodes(hasText(str(id))).fetchSemanticsNodes().isNotEmpty()) return
            Thread.sleep(FRAME_MILLIS)
        }
        throw AssertionError("'${str(id)}' did not appear within ${timeoutMillis}ms")
    }

    /** What accessibility services such as TalkBack actually see (not Compose's own semantics tree). */
    private fun accessibilityNodes(): List<AccessibilityNodeInfo> {
        compose.waitForIdle()
        val root = InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow ?: return emptyList()
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        fun walk(node: AccessibilityNodeInfo) {
            nodes += node
            for (i in 0 until node.childCount) node.getChild(i)?.let(::walk)
        }
        walk(root)
        return nodes
    }

    /**
     * Polls the accessibility tree until [find] returns a result: Compose builds the tree only once an
     * accessibility client connects and sends updates asynchronously.
     */
    private fun <T : Any> awaitAccessibility(what: String, timeoutMillis: Long = 5_000, find: (List<AccessibilityNodeInfo>) -> T?): T {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (true) {
            find(accessibilityNodes())?.let { return it }
            if (SystemClock.uptimeMillis() > deadline) throw AssertionError("Accessibility tree never showed $what")
            Thread.sleep(50)
        }
    }

    private fun accessibilityNode(description: String): AccessibilityNodeInfo =
        awaitAccessibility("'$description'") { nodes -> nodes.singleOrNull { it.contentDescription?.toString() == description } }

    @Test
    fun boardPointsAreExposedToAccessibilityServicesAndActivatable() {
        startTwoPlayer()
        val pointPrefix = str(R.string.point_description, 1, 1, "").substringBefore("1")
        val pointDescriptions = awaitAccessibility("the board points") { nodes ->
            nodes.mapNotNull { it.contentDescription?.toString() }.filter { it.startsWith(pointPrefix) }.takeIf { it.size >= 25 }
        }
        assertEquals("one accessibility node per point", 25, pointDescriptions.size)
        assertEquals(str(R.string.point_description, 1, 1, str(R.string.point_tiger)), pointDescriptions.first())

        val centre = accessibilityNode(str(R.string.point_description, 3, 3, str(R.string.point_empty)))
        assertTrue(
            "point offers a click action",
            centre.actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK },
        )
        assertTrue(centre.performAction(AccessibilityNodeInfo.ACTION_CLICK))

        waitForText(R.string.turn_tigers)
        accessibilityNode(str(R.string.point_description, 3, 3, str(R.string.point_goat)))
    }

    // --- Result screen: resume a saved game one move from the end, then play that move ---

    private fun resumeSaved(game: GameState) {
        val container = ApplicationProvider.getApplicationContext<BaghchalApplication>().container
        runBlocking { container.savedGames.save(SavedGame(game, aiSide = null, difficulty = Difficulty.MEDIUM)) }
        waitForText(R.string.home_resume)
        text(R.string.home_resume).performClick()
    }

    private fun moves(count: Int) = compose.activity.resources.getQuantityString(R.plurals.result_moves, count, count)

    @Test
    fun goatsWinShowsTheResultScreenAndPlayAgainStartsAFreshGame() {
        // Goat 7 -> 2 traps all four tigers.
        resumeSaved(
            GameState.create(
                tigers = listOf(0, 4, 20, 24),
                goats = listOf(1, 3, 5, 6, 7, 8, 9, 10, 12, 14, 15, 16, 18, 19, 21, 22, 23),
                sideToMove = Side.GOAT,
                goatsInHand = 0,
                goatsCaptured = 3,
                moveCount = 40,
            ),
        )
        waitForText(R.string.turn_goats)
        point(20).assertContentDescriptionEquals(str(R.string.point_description, 5, 1, str(R.string.point_tiger_trapped)))
        point(7).performClick()
        point(2).performClick()

        waitForText(R.string.result_title_goats)
        compose.onNodeWithText(moves(41)).assertIsDisplayed()

        text(R.string.play_again).performClick()
        waitForText(R.string.turn_goats)
        text(R.string.goats_in_hand, 20).assertIsDisplayed()

        Espresso.pressBack()
        waitForText(R.string.home_vs_computer)
        text(R.string.home_resume).assertDoesNotExist()
    }

    @Test
    fun tigersWinShowsTheResultScreenAndHomeGoesBack() {
        val goats = listOf(13, 1, 2, 3, 5, 6, 8, 9, 10, 15, 16, 17, 19, 21, 22, 23)
        resumeSaved(GameState.create(listOf(12, 4, 20, 24), goats, Side.TIGER, goatsInHand = 0, goatsCaptured = 4, moveCount = 9))
        waitForText(R.string.turn_tigers)
        point(12).performClick()
        point(14).performClick() // fifth capture

        waitForText(R.string.result_title_tigers)
        compose.onNodeWithText(moves(10)).assertIsDisplayed()

        text(R.string.home).performClick()
        waitForText(R.string.home_vs_computer)
        text(R.string.home_resume).assertDoesNotExist()
    }

    @Test
    fun backFromTheResultScreenGoesHome() {
        val goats = listOf(13, 1, 2, 3, 5, 6, 8, 9, 10, 15, 16, 17, 19, 21, 22, 23)
        resumeSaved(GameState.create(listOf(12, 4, 20, 24), goats, Side.TIGER, goatsInHand = 0, goatsCaptured = 4))
        waitForText(R.string.turn_tigers)
        point(12).performClick()
        point(14).performClick()
        waitForText(R.string.result_title_tigers)

        Espresso.pressBack()
        waitForText(R.string.home_vs_computer)
    }

    @Test
    fun leavingAGameOffersResumeWhichRestoresTheBoard() {
        startTwoPlayer()
        point(6).performClick()
        waitForText(R.string.turn_tigers)

        Espresso.pressBack()
        waitForText(R.string.home_resume)
        text(R.string.home_resume_progress, 19, 0, 5).assertIsDisplayed()

        text(R.string.home_resume).performClick()
        waitForText(R.string.turn_tigers)
        point(6).assertContentDescriptionEquals(str(R.string.point_description, 2, 2, str(R.string.point_goat)))
    }
}
