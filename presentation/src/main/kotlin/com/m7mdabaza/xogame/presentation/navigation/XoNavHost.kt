package com.m7mdabaza.xogame.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.m7mdabaza.xogame.presentation.achievements.AchievementsRoute
import com.m7mdabaza.xogame.presentation.challenge.DailyChallengeRoute
import com.m7mdabaza.xogame.presentation.game.GameRoute
import com.m7mdabaza.xogame.presentation.home.HomeRoute
import com.m7mdabaza.xogame.presentation.settings.SettingsRoute
import com.m7mdabaza.xogame.presentation.stats.StatsRoute
import com.m7mdabaza.xogame.presentation.twophones.TwoPhonesGameRoute
import com.m7mdabaza.xogame.presentation.twophones.TwoPhonesLobbyRoute
import kotlinx.serialization.Serializable

/** Type-safe navigation destinations. */
sealed interface Destination {
    @Serializable
    data object Home : Destination

    /**
     * A game. [mode], [boardSize] and [level] are `GameMode` / `BoardSize` /
     * `Level` names ([level] is null in two-player mode); plain strings so
     * `GameViewModel` can read them from its `SavedStateHandle` (the property
     * names must match `GameViewModel.ARG_MODE` / `ARG_BOARD_SIZE` / `ARG_LEVEL`).
     */
    @Serializable
    data class Game(val mode: String, val boardSize: String, val level: String?) : Destination

    @Serializable
    data object Stats : Destination

    @Serializable
    data object Settings : Destination

    @Serializable
    data object DailyChallenge : Destination

    @Serializable
    data object Achievements : Destination

    /** Find a friend nearby (two phones). */
    @Serializable
    data object TwoPhonesLobby : Destination

    /** A two-phone game over the connection the lobby made. */
    @Serializable
    data object TwoPhonesGame : Destination
}

/**
 * @param openDailyChallenge a tapped reminder asks for the Daily challenge:
 *   it opens on top of Home (Back returns to Home), then
 *   [onDailyChallengeOpened] clears the request.
 */
@Composable
fun XoNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    openDailyChallenge: Boolean = false,
    onDailyChallengeOpened: () -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = Destination.Home,
        modifier = modifier,
    ) {
        composable<Destination.Home> {
            HomeRoute(
                onStartGame = { mode, boardSize, level ->
                    navController.navigate(Destination.Game(mode.name, boardSize.name, level?.name)) { launchSingleTop = true }
                },
                onOpenStats = { navController.navigate(Destination.Stats) { launchSingleTop = true } },
                onOpenSettings = { navController.navigate(Destination.Settings) { launchSingleTop = true } },
                onOpenChallenge = { navController.navigate(Destination.DailyChallenge) { launchSingleTop = true } },
                onOpenAchievements = { navController.navigate(Destination.Achievements) { launchSingleTop = true } },
                onOpenTwoPhones = { navController.navigate(Destination.TwoPhonesLobby) { launchSingleTop = true } },
            )
        }
        composable<Destination.Game> {
            GameRoute(onBack = { navController.popBackStack(Destination.Home, inclusive = false) })
        }
        composable<Destination.Stats> {
            StatsRoute(onBack = { navController.popBackStack(Destination.Home, inclusive = false) })
        }
        composable<Destination.Settings> {
            SettingsRoute(onBack = { navController.popBackStack() })
        }
        composable<Destination.DailyChallenge> {
            DailyChallengeRoute(onBack = { navController.popBackStack(Destination.Home, inclusive = false) })
        }
        composable<Destination.Achievements> {
            AchievementsRoute(onBack = { navController.popBackStack(Destination.Home, inclusive = false) })
        }
        composable<Destination.TwoPhonesLobby> {
            TwoPhonesLobbyRoute(
                onBack = { navController.popBackStack(Destination.Home, inclusive = false) },
                // The lobby goes away: Back from the game returns to Home.
                onConnected = {
                    navController.navigate(Destination.TwoPhonesGame) {
                        popUpTo<Destination.Home>()
                        launchSingleTop = true
                    }
                },
            )
        }
        composable<Destination.TwoPhonesGame> {
            TwoPhonesGameRoute(onExit = { navController.popBackStack(Destination.Home, inclusive = false) })
        }
    }
    // After NavHost, so the graph is set.
    LaunchedEffect(openDailyChallenge) {
        if (!openDailyChallenge) return@LaunchedEffect
        navController.navigate(Destination.DailyChallenge) {
            popUpTo<Destination.Home>()
            launchSingleTop = true
        }
        onDailyChallengeOpened()
    }
}
