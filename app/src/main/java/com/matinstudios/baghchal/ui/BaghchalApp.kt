package com.matinstudios.baghchal.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.matinstudios.baghchal.ui.screens.GameScreen
import com.matinstudios.baghchal.ui.screens.GameSetup
import com.matinstudios.baghchal.ui.screens.HomeScreen

private const val HOME_ROUTE = "home"

@Composable
fun BaghchalApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = HOME_ROUTE, modifier = modifier) {
        composable(HOME_ROUTE) {
            HomeScreen(onStartGame = { setup -> navController.navigate(setup.route) })
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
            ),
        ) {
            GameScreen(onMenu = { navController.popBackStack(HOME_ROUTE, inclusive = false) })
        }
    }
}
