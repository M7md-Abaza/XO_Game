package com.m7mdabaza.xogame.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.m7mdabaza.xogame.presentation.game.GameRoute
import com.m7mdabaza.xogame.presentation.home.HomeRoute
import kotlinx.serialization.Serializable

/** Type-safe navigation destinations. Stats, Settings, ... are added in later milestones. */
sealed interface Destination {
    @Serializable
    data object Home : Destination

    /**
     * A game. [mode] and [level] are `GameMode` / `Level` names; plain strings
     * so `GameViewModel` can read them from its `SavedStateHandle` (the
     * property names must match `GameViewModel.ARG_MODE` / `ARG_LEVEL`).
     */
    @Serializable
    data class Game(val mode: String, val level: String) : Destination
}

@Composable
fun XoNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Destination.Home,
        modifier = modifier,
    ) {
        composable<Destination.Home> {
            HomeRoute(
                onStartGame = { mode, level ->
                    navController.navigate(Destination.Game(mode.name, level.name)) { launchSingleTop = true }
                },
            )
        }
        composable<Destination.Game> {
            GameRoute(onBack = { navController.popBackStack(Destination.Home, inclusive = false) })
        }
    }
}
