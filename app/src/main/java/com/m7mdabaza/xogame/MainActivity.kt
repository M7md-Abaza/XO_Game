package com.m7mdabaza.xogame

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.m7mdabaza.xogame.presentation.XoApp
import com.m7mdabaza.xogame.presentation.reminder.ACTION_OPEN_DAILY_CHALLENGE
import com.m7mdabaza.xogame.presentation.reminder.ChallengeReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

/**
 * Single Compose activity. AppCompat-based so per-app language switching
 * works on Android 7–12 too (milestone 5).
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var reminderScheduler: ChallengeReminderScheduler

    /** A tapped reminder asked for the Daily challenge; cleared once it is open. */
    private val openDailyChallenge = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        // Only a fresh start: a recreated activity (new theme or language, process
        // death) still has the intent it was opened with, already handled.
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            val openChallenge by openDailyChallenge.collectAsState()
            XoApp(openDailyChallenge = openChallenge, onDailyChallengeOpened = { openDailyChallenge.value = false })
        }
    }

    /** A reminder tapped while the app was already running (see the splash activity). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        // "Opened the app": the next daily-challenge reminder is due 24 hours from now.
        reminderScheduler.onAppOpened()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_OPEN_DAILY_CHALLENGE) openDailyChallenge.value = true
    }
}
