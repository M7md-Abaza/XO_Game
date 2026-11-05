package com.m7mdabaza.xogame.di

import android.content.Context
import android.content.Intent
import com.m7mdabaza.xogame.presentation.reminder.ACTION_OPEN_DAILY_CHALLENGE
import com.m7mdabaza.xogame.presentation.reminder.AppIntents
import com.m7mdabaza.xogame.startActivities.splash
import javax.inject.Inject

/**
 * Opens the Daily challenge through the launch screen, which runs the consent
 * flow on a cold start and forwards to a running MainActivity otherwise. The
 * action (not the launcher's) keeps Android from just bringing an existing
 * task to the front without delivering the intent.
 */
class AppIntentsImpl @Inject constructor() : AppIntents {
    override fun openDailyChallenge(context: Context): Intent =
        Intent(context, splash::class.java)
            .setAction(ACTION_OPEN_DAILY_CHALLENGE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
