package com.m7mdabaza.xogame.presentation

import androidx.compose.runtime.Composable
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.Level
import com.m7mdabaza.xogame.presentation.navigation.XoNavHost
import com.m7mdabaza.xogame.presentation.theme.XoTheme

/** Root of the Compose UI, hosted by MainActivity in :app. */
@Composable
fun XoApp(onStartGame: (GameMode, Level) -> Unit) {
    XoTheme {
        XoNavHost(onStartGame = onStartGame)
    }
}
