package com.matinstudios.baghchal.ui

import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
import com.matinstudios.baghchal.data.PieceSet
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
                container.entitlements.reset()
                container.settings.setTutorialSeen(true)
            }
        }
    }

    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(ResetAppData()).around(compose)

    private fun str(@StringRes id: Int, vararg args: Any) = compose.activity.getString(id, *args)

    private fun text(@StringRes id: Int, vararg args: Any): SemanticsNodeInteraction =
        compose.onNodeWithText(str(id, *args))

    /** Opens the Home side menu and picks [item]. */
    private fun openMenuItem(@StringRes item: Int) {
        compose.onNodeWithContentDescription(str(R.string.home_menu)).performClick()
        text(item).performClick()
    }

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
        text(R.string.mode_vs_player).performScrollTo().performClick()
        waitForText(R.string.turn_goats)
    }

    @Test
    fun gameMenuOpensSettingsAndKeepsTheGame() {
        startTwoPlayer()
        point(12).performClick() // a goat goes in the centre, so it's the tigers' turn
        waitForText(R.string.turn_tigers)

        openMenuItem(R.string.home_settings)
        waitForText(R.string.settings_sound)
        Espresso.pressBack()
        waitForText(R.string.turn_tigers)

        openMenuItem(R.string.menu_home)
        waitForText(R.string.home_resume)
    }

    @Test
    fun menuListsThePrivacyPolicy() {
        compose.onNodeWithContentDescription(str(R.string.home_menu)).performClick()
        waitForText(R.string.menu_privacy_policy)
    }

    @Test
    fun playOnlineSaysItIsUnavailableWithoutAServer() {
        // Instrumented tests run without online play, like a build without a Firebase config.
        text(R.string.mode_online).performScrollTo().performClick()
        waitForText(R.string.online_unavailable)
        Espresso.pressBack()
        waitForText(R.string.mode_vs_player)
    }

    @Test
    fun playWithAFriendSaysItIsUnavailableWithoutAServer() {
        text(R.string.mode_friend).performScrollTo().performClick()
        waitForText(R.string.online_unavailable)
        Espresso.pressBack()
        waitForText(R.string.mode_vs_player)
    }

    @Test
    fun homeToTwoPlayerGame() {
        text(R.string.mode_vs_ai).performScrollTo().assertIsDisplayed()
        startTwoPlayer()
        text(R.string.goats_in_hand, 20).assertIsDisplayed()
        point(0).assertContentDescriptionEquals(str(R.string.point_description, 1, 1, str(R.string.point_tiger)))
    }

    @Test
    fun playerVsAiOffersSidesAndDifficultiesAndBackReturnsHome() {
        text(R.string.mode_vs_ai).performScrollTo().performClick()
        waitForText(R.string.ai_setup_start)
        text(R.string.side_goats).assertIsSelected()
        text(R.string.difficulty_medium).assertIsSelected()
        text(R.string.difficulty_easy).performClick()
        text(R.string.difficulty_easy).assertIsSelected()
        text(R.string.difficulty_medium).assertIsNotSelected()

        compose.onNodeWithContentDescription(str(R.string.back)).performClick()
        waitForText(R.string.mode_vs_player)
    }

    @Test
    fun homeToHowToPlayAndBack() {
        openMenuItem(R.string.home_how_to_play)
        waitForText(R.string.rules_goal)
        text(R.string.rules_goal).assertIsDisplayed()
        Espresso.pressBack()
        waitForText(R.string.mode_vs_player)
    }

    @Test
    fun menuOpensStatsAndBackClosesIt() {
        val menuStats = text(R.string.home_stats) // the closed drawer stays in the tree, off-screen
        compose.onNodeWithContentDescription(str(R.string.home_menu)).performClick()
        menuStats.assertIsDisplayed()
        Espresso.pressBack()
        compose.waitUntil(5_000) { !menuStats.isDisplayed() }

        openMenuItem(R.string.home_stats)
        waitForText(R.string.stats_subtitle)
        Espresso.pressBack()
        waitForText(R.string.mode_vs_player)
        menuStats.assertIsNotDisplayed()
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
        text(R.string.mode_vs_ai).performScrollTo().performClick()
        waitForText(R.string.ai_setup_start)
        text(R.string.side_tigers).performClick()
        text(R.string.difficulty_hard).performScrollTo().performClick()
        text(R.string.ai_setup_start).performScrollTo()
        // The spinner animates forever, so Compose is never idle while it shows: drive frames by hand.
        compose.mainClock.autoAdvance = false
        text(R.string.ai_setup_start).performClick()

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
        waitForText(R.string.mode_vs_player)
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
        waitForText(R.string.mode_vs_player)
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
        waitForText(R.string.mode_vs_player)
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

    @Test
    fun undoTakesBackTheLastMoveForFreeWithoutAds() {
        startTwoPlayer()
        point(12).performClick()
        waitForText(R.string.turn_tigers)

        // Tests run without ads, so Undo is free and plays no ad.
        text(R.string.undo).performClick()
        waitForText(R.string.turn_goats)
        text(R.string.goats_in_hand, 20).assertIsDisplayed()
        point(12).assertContentDescriptionEquals(str(R.string.point_description, 3, 3, str(R.string.point_empty)))
    }

    @Test
    fun hintSuggestsAMoveOnTheBoard() {
        startTwoPlayer()
        text(R.string.hint).performClick()
        compose.waitUntil(10_000) {
            val isHintTarget = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, str(R.string.point_hint_to))
            compose.onAllNodes(isHintTarget).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun lockedBoardsNeedAnAdAndSayWhenNoneIsAvailable() {
        openMenuItem(R.string.menu_boards)
        // Every board but Wood and every piece set but Classic is locked. (The title is also the menu
        // item's text, so wait for the tiles.)
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText(str(R.string.boards_watch_ad)).fetchSemanticsNodes().size ==
                BoardTheme.entries.size - 1 + PieceSet.entries.size - 1
        }

        // Tests run without ads: a permanent unlock needs a watched ad, so Stone stays locked.
        text(R.string.board_stone).performClick()
        waitForText(R.string.message_no_ad)
        text(R.string.board_wood).assertIsSelected()
        text(R.string.board_stone).assertIsNotSelected()

        // Piece sets unlock the same way.
        text(R.string.piece_set_festival).performScrollTo().performClick()
        waitForText(R.string.message_no_ad)
        text(R.string.piece_set_festival).assertIsNotSelected()
    }

    @Test
    fun removeAdsSaysWhenGooglePlayIsUnavailable() {
        openMenuItem(R.string.home_settings)
        waitForText(R.string.settings_title)
        // Tests run without Play Billing, so the purchase sheet can't open.
        compose.onNode(hasText(str(R.string.settings_remove_ads)) and hasClickAction()).performScrollTo().performClick()
        waitForText(R.string.message_purchase_unavailable)
        text(R.string.settings_restore_purchase).assertIsDisplayed()
    }
}
