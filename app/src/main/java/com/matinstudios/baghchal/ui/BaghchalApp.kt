package com.matinstudios.baghchal.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.matinstudios.baghchal.ui.screens.AiSetupScreen
import com.matinstudios.baghchal.ui.screens.BoardsScreen
import com.matinstudios.baghchal.ui.screens.GameResult
import com.matinstudios.baghchal.ui.screens.GameScreen
import com.matinstudios.baghchal.ui.screens.GameSetup
import com.matinstudios.baghchal.ui.screens.HomeScreen
import com.matinstudios.baghchal.ui.screens.HowToPlayScreen
import com.matinstudios.baghchal.ui.screens.OnlineGameScreen
import com.matinstudios.baghchal.ui.screens.OnlineGameViewModel
import com.matinstudios.baghchal.ui.screens.OnlineLobbyMode
import com.matinstudios.baghchal.ui.screens.OnlineLobbyScreen
import com.matinstudios.baghchal.ui.screens.ResultScreen
import com.matinstudios.baghchal.ui.screens.ResultViewModel
import com.matinstudios.baghchal.ui.screens.SettingsScreen
import com.matinstudios.baghchal.ui.screens.ShopScreen
import com.matinstudios.baghchal.ui.screens.ShopSection
import com.matinstudios.baghchal.ui.screens.StatsScreen
import com.matinstudios.baghchal.ui.screens.WelcomeScreen

private const val HOME_ROUTE = "home"
private const val STATS_ROUTE = "stats"
private const val SETTINGS_ROUTE = "settings"
private const val SHOP_ROUTE = "shop"
private const val ARG_SHOP_SECTION = "section"
private const val SHOP_SECTION_ROUTE = "shop/{$ARG_SHOP_SECTION}"
private const val HOW_TO_PLAY_ROUTE = "how_to_play"
private const val WELCOME_ROUTE = "welcome"
private const val AI_SETUP_ROUTE = "ai_setup"
private const val ARG_ONLINE_MODE = "mode"
private const val ONLINE_ROUTE = "online/{$ARG_ONLINE_MODE}"

private fun onlineRoute(mode: OnlineLobbyMode) = "online/${mode.name}"

private fun shopSectionRoute(section: ShopSection) = "shop/${section.name}"

/**
 * A screen kept inside the system bars by [barPadding]. Home is the only one that is not: it draws its
 * night sky behind the status bar and keeps its own content clear of the bars.
 */
private fun NavGraphBuilder.insetScreen(
    route: String,
    barPadding: PaddingValues,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable (NavBackStackEntry) -> Unit,
) {
    composable(route, arguments) { entry ->
        Box(Modifier.fillMaxSize().padding(barPadding)) { content(entry) }
    }
}

/** [barPadding]: the space the status and navigation bars take (from the activity's Scaffold). */
@Composable
fun BaghchalApp(
    barPadding: PaddingValues,
    modifier: Modifier = Modifier,
    appViewModel: AppViewModel = viewModel(factory = AppViewModel.Factory),
) {
    // Blank for the moment it takes to read whether the tutorial was seen.
    val showWelcome = appViewModel.showWelcome.collectAsStateWithLifecycle().value ?: return
    val navController = rememberNavController()
    NavHost(navController, startDestination = if (showWelcome) WELCOME_ROUTE else HOME_ROUTE, modifier = modifier) {
        insetScreen(WELCOME_ROUTE, barPadding) {
            WelcomeScreen(
                onFinished = {
                    navController.navigate(HOME_ROUTE) {
                        popUpTo(WELCOME_ROUTE) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(HOME_ROUTE) {
            HomeScreen(
                barPadding = barPadding,
                onPlayVsAi = { navController.navigate(AI_SETUP_ROUTE) },
                onPlayOnline = { navController.navigate(onlineRoute(OnlineLobbyMode.RANDOM)) },
                onPlayWithFriend = { navController.navigate(onlineRoute(OnlineLobbyMode.FRIEND)) },
                onStartGame = { setup -> navController.navigate(setup.route) },
                onResume = { navController.navigate(GameSetup.RESUME_ROUTE) },
                onStats = { navController.navigate(STATS_ROUTE) },
                onShop = { navController.navigate(SHOP_ROUTE) },
                onSettings = { navController.navigate(SETTINGS_ROUTE) },
                onHowToPlay = { navController.navigate(HOW_TO_PLAY_ROUTE) },
            )
        }
        insetScreen(
            GameSetup.ROUTE_PATTERN,
            barPadding,
            arguments = listOf(
                navArgument(GameSetup.ARG_AI) {
                    type = NavType.StringType
                    defaultValue = GameSetup.NO_AI
                },
                navArgument(GameSetup.ARG_DIFFICULTY) {
                    type = NavType.StringType
                    defaultValue = GameSetup.TWO_PLAYER.difficulty.name
                },
                navArgument(GameSetup.ARG_RESUME) {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) {
            GameScreen(
                onMenu = { navController.popBackStack(HOME_ROUTE, inclusive = false) },
                // The game is over, so Back from the result goes Home rather than to the finished board.
                onGameWon = { result -> navController.navigate(result.route) { popUpTo(HOME_ROUTE) } },
                onHowToPlay = { navController.navigate(HOW_TO_PLAY_ROUTE) },
                onStats = { navController.navigate(STATS_ROUTE) },
                onSettings = { navController.navigate(SETTINGS_ROUTE) },
            )
        }
        insetScreen(
            GameResult.ROUTE_PATTERN,
            barPadding,
            arguments = listOf(
                navArgument(GameResult.ARG_WINNER) { type = NavType.StringType },
                navArgument(GameResult.ARG_MOVES) { type = NavType.IntType },
                navArgument(GameSetup.ARG_AI) {
                    type = NavType.StringType
                    defaultValue = GameSetup.NO_AI
                },
                navArgument(GameSetup.ARG_DIFFICULTY) {
                    type = NavType.StringType
                    defaultValue = GameSetup.TWO_PLAYER.difficulty.name
                },
            ),
        ) { entry ->
            val args = entry.arguments
            val result = GameResult.fromArgs(
                winner = args?.getString(GameResult.ARG_WINNER),
                moves = args?.getInt(GameResult.ARG_MOVES) ?: 0,
                ai = args?.getString(GameSetup.ARG_AI),
                difficulty = args?.getString(GameSetup.ARG_DIFFICULTY),
            )
            val resultViewModel: ResultViewModel = viewModel(factory = ResultViewModel.Factory)
            ResultScreen(
                result = result,
                onPlayAgain = {
                    resultViewModel.onLeave()
                    navController.navigate(result.setup.route) { popUpTo(HOME_ROUTE) }
                },
                onHome = {
                    resultViewModel.onLeave()
                    navController.popBackStack(HOME_ROUTE, inclusive = false)
                },
            )
        }
        insetScreen(AI_SETUP_ROUTE, barPadding) {
            AiSetupScreen(
                // Back from the game goes Home, where the game can be resumed.
                onStart = { setup -> navController.navigate(setup.route) { popUpTo(HOME_ROUTE) } },
                onBack = { navController.popBackStack() },
            )
        }
        insetScreen(
            ONLINE_ROUTE,
            barPadding,
            arguments = listOf(navArgument(ARG_ONLINE_MODE) { type = NavType.StringType }),
        ) { entry ->
            val mode = entry.arguments?.getString(ARG_ONLINE_MODE)
                ?.let { name -> OnlineLobbyMode.entries.firstOrNull { it.name == name } }
                ?: OnlineLobbyMode.RANDOM
            OnlineLobbyScreen(
                mode = mode,
                onMatched = { roomId -> navController.navigate(OnlineGameViewModel.route(roomId)) },
                onBack = { navController.popBackStack() },
            )
        }
        insetScreen(
            OnlineGameViewModel.ROUTE_PATTERN,
            barPadding,
            arguments = listOf(navArgument(OnlineGameViewModel.ARG_ROOM) { type = NavType.StringType }),
        ) {
            OnlineGameScreen(
                onHome = { navController.popBackStack(HOME_ROUTE, inclusive = false) },
                onPlayAgain = { navController.popBackStack(ONLINE_ROUTE, inclusive = false) },
                // The finished game makes way for the next one, so Back never returns to it.
                onNextGame = { roomId ->
                    navController.navigate(OnlineGameViewModel.route(roomId)) {
                        popUpTo(OnlineGameViewModel.ROUTE_PATTERN) { inclusive = true }
                    }
                },
                onHowToPlay = { navController.navigate(HOW_TO_PLAY_ROUTE) },
                onStats = { navController.navigate(STATS_ROUTE) },
                onSettings = { navController.navigate(SETTINGS_ROUTE) },
            )
        }
        insetScreen(STATS_ROUTE, barPadding) {
            StatsScreen(onBack = { navController.popBackStack() })
        }
        insetScreen(SHOP_ROUTE, barPadding) {
            ShopScreen(
                onOpenSection = { section -> navController.navigate(shopSectionRoute(section)) },
                onBack = { navController.popBackStack() },
            )
        }
        insetScreen(
            SHOP_SECTION_ROUTE,
            barPadding,
            arguments = listOf(navArgument(ARG_SHOP_SECTION) { type = NavType.StringType }),
        ) { entry ->
            val section = entry.arguments?.getString(ARG_SHOP_SECTION)
                ?.let { name -> ShopSection.entries.firstOrNull { it.name == name } }
                ?: ShopSection.BOARDS
            BoardsScreen(section = section, onBack = { navController.popBackStack() })
        }
        insetScreen(SETTINGS_ROUTE, barPadding) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        insetScreen(HOW_TO_PLAY_ROUTE, barPadding) {
            HowToPlayScreen(onBack = { navController.popBackStack() })
        }
    }
}
