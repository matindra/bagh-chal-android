package com.matinstudios.baghchal.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.matinstudios.baghchal.ui.screens.AiSetupScreen
import com.matinstudios.baghchal.ui.screens.GameResult
import com.matinstudios.baghchal.ui.screens.GameScreen
import com.matinstudios.baghchal.ui.screens.GameSetup
import com.matinstudios.baghchal.ui.screens.HomeScreen
import com.matinstudios.baghchal.ui.screens.HowToPlayScreen
import com.matinstudios.baghchal.ui.screens.ResultScreen
import com.matinstudios.baghchal.ui.screens.ResultViewModel
import com.matinstudios.baghchal.ui.screens.SettingsScreen
import com.matinstudios.baghchal.ui.screens.StatsScreen
import com.matinstudios.baghchal.ui.screens.WelcomeScreen

private const val HOME_ROUTE = "home"
private const val STATS_ROUTE = "stats"
private const val SETTINGS_ROUTE = "settings"
private const val HOW_TO_PLAY_ROUTE = "how_to_play"
private const val WELCOME_ROUTE = "welcome"
private const val AI_SETUP_ROUTE = "ai_setup"

@Composable
fun BaghchalApp(modifier: Modifier = Modifier, appViewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)) {
    // Blank for the moment it takes to read whether the tutorial was seen.
    val showWelcome = appViewModel.showWelcome.collectAsStateWithLifecycle().value ?: return
    val navController = rememberNavController()
    NavHost(navController, startDestination = if (showWelcome) WELCOME_ROUTE else HOME_ROUTE, modifier = modifier) {
        composable(WELCOME_ROUTE) {
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
                onPlayVsAi = { navController.navigate(AI_SETUP_ROUTE) },
                onStartGame = { setup -> navController.navigate(setup.route) },
                onResume = { navController.navigate(GameSetup.RESUME_ROUTE) },
                onStats = { navController.navigate(STATS_ROUTE) },
                onSettings = { navController.navigate(SETTINGS_ROUTE) },
                onHowToPlay = { navController.navigate(HOW_TO_PLAY_ROUTE) },
            )
        }
        composable(
            GameSetup.ROUTE_PATTERN,
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
            )
        }
        composable(
            GameResult.ROUTE_PATTERN,
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
        composable(AI_SETUP_ROUTE) {
            AiSetupScreen(
                // Back from the game goes Home, where the game can be resumed.
                onStart = { setup -> navController.navigate(setup.route) { popUpTo(HOME_ROUTE) } },
                onBack = { navController.popBackStack() },
            )
        }
        composable(STATS_ROUTE) {
            StatsScreen(onBack = { navController.popBackStack() })
        }
        composable(SETTINGS_ROUTE) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(HOW_TO_PLAY_ROUTE) {
            HowToPlayScreen(onBack = { navController.popBackStack() })
        }
    }
}
