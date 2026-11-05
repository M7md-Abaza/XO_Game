package com.m7mdabaza.xogame.presentation.reminder

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Android 13+: asks for the notification permission (for the daily-challenge
 * reminder) when [shouldAsk] says so, which is once ever, on Home at startup
 * (user decision). Does nothing below Android 13, where no permission is needed.
 */
@Composable
fun AskNotificationPermissionOnce(shouldAsk: suspend () -> Boolean, onAsked: suspend () -> Unit) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    // The answer needs no handling: the reminder checks the permission when it is due.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (hasNotificationPermission(context) || !shouldAsk()) return@LaunchedEffect
        onAsked()
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
