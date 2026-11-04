package com.m7mdabaza.xogame.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.Level
import com.m7mdabaza.xogame.presentation.home.HomeRoute
import kotlinx.serialization.Serializable

/** Type-safe navigation destinations. Game, Stats, Settings, ... are added in later milestones. */
sealed interface Destination {
    @Serializable
    data object Home : Destination
}

/**
 * @param onStartGame opens a game. Until the Compose game screen exists
 *   (milestone 4) the app module launches the legacy XML game screens here.
 */
@Composable
fun XoNavHost(
    onStartGame: (GameMode, Level) -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Destination.Home,
        modifier = modifier,
    ) {
        composable<Destination.Home> {
            HomeRoute(onStartGame = onStartGame)
        }
    }
}
