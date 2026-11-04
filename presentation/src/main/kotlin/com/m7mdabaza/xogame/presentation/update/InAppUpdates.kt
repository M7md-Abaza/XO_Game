package com.m7mdabaza.xogame.presentation.update

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.ktx.AppUpdateResult
import com.google.android.play.core.ktx.isFlexibleUpdateAllowed
import com.google.android.play.core.ktx.requestUpdateFlow
import com.m7mdabaza.xogame.presentation.R
import kotlinx.coroutines.flow.catch

/**
 * Flexible Google Play in-app updates. When Play has a newer version, Play's
 * own dialog offers it ([shouldPrompt] limits how often); it downloads while
 * the player keeps playing, then [snackbar] offers "Restart" to install it. A
 * download finished earlier is offered again on the next start. Without Play
 * (emulators, sideloaded builds) the check fails quietly.
 */
@Composable
fun InAppUpdates(
    snackbar: SnackbarHostState,
    shouldPrompt: suspend () -> Boolean,
    onPrompted: suspend () -> Unit,
    manager: AppUpdateManager = rememberAppUpdateManager(),
) {
    // Play's dialog reports accept/decline here; the flow below follows the download either way.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { }
    val message = stringResource(R.string.update_downloaded)
    val restart = stringResource(R.string.update_restart)

    LaunchedEffect(manager) {
        var offered = false
        manager.requestUpdateFlow()
            .catch { /* Play unavailable or not installed from Play: nothing to offer. */ }
            .collect { result ->
                when (result) {
                    is AppUpdateResult.Available ->
                        if (!offered && result.updateInfo.isFlexibleUpdateAllowed && shouldPrompt()) {
                            offered = true
                            onPrompted()
                            result.startFlexibleUpdate(launcher)
                        }
                    is AppUpdateResult.Downloaded -> {
                        val choice = snackbar.showSnackbar(
                            message = message,
                            actionLabel = restart,
                            withDismissAction = true,
                            duration = SnackbarDuration.Indefinite,
                        )
                        if (choice == SnackbarResult.ActionPerformed) result.completeUpdate()
                    }
                    else -> Unit
                }
            }
    }
}

@Composable
private fun rememberAppUpdateManager(): AppUpdateManager {
    val context = LocalContext.current.applicationContext
    return remember(context) { AppUpdateManagerFactory.create(context) }
}
