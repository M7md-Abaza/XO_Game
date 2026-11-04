package com.m7mdabaza.xogame.presentation

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.m7mdabaza.xogame.domain.model.ThemeMode
import com.m7mdabaza.xogame.domain.usecase.ObserveSettingsUseCase
import com.m7mdabaza.xogame.presentation.navigation.XoNavHost
import com.m7mdabaza.xogame.presentation.splash.SplashContent
import com.m7mdabaza.xogame.presentation.theme.XoTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** App-wide state for [XoApp]: the theme setting. */
@HiltViewModel
class XoAppViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = observeSettings()
        .map { it.theme }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)
}

/** Root of the Compose UI, hosted by MainActivity in :app. */
@Composable
fun XoApp(viewModel: XoAppViewModel = hiltViewModel()) {
    XoRoot(viewModel) { XoNavHost() }
}

/** The launch screen, hosted by the splash activity in :app (which runs the consent flow). */
@Composable
fun XoSplash(viewModel: XoAppViewModel = hiltViewModel()) {
    XoRoot(viewModel) { SplashContent() }
}

/** The app theme (from the theme setting), matching system-bar icons and the base surface. */
@Composable
private fun XoRoot(viewModel: XoAppViewModel, content: @Composable () -> Unit) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    SystemBarIcons(darkTheme)
    XoTheme(darkTheme = darkTheme) {
        // The Surface sets the default text/icon colour (onBackground) for every screen,
        // which matters in the dark theme.
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

/**
 * Light or dark status/navigation bar icons to match the app theme, which
 * can differ from the system theme.
 */
@Composable
private fun SystemBarIcons(darkTheme: Boolean) {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    DisposableEffect(activity, darkTheme) {
        val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
        activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        onDispose { }
    }
}
