package com.m7mdabaza.xogame.presentation.reminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.m7mdabaza.xogame.domain.model.ChallengeReminder
import com.m7mdabaza.xogame.presentation.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** The intent action that opens the app on the Daily challenge (a tapped reminder). */
const val ACTION_OPEN_DAILY_CHALLENGE = "com.m7mdabaza.xogame.action.OPEN_DAILY_CHALLENGE"

/** Intents for the app's activities, which live in :app (bound there, like `AdUnitIds`). */
interface AppIntents {
    /**
     * Opens the app on the Daily challenge, through the launch screen (which
     * runs the consent flow), with [ACTION_OPEN_DAILY_CHALLENGE].
     */
    fun openDailyChallenge(context: Context): Intent
}

private const val CHANNEL_ID = "daily_challenge"
private const val NOTIFICATION_ID = 1
private const val BRAND_YELLOW = 0xFFD6BD17.toInt()

/** Android 13+: the user allowed notifications. Always true below 13. */
fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

/**
 * Whether a reminder would actually show: permission granted (Android 13+),
 * the app's notifications on, and the reminder channel not turned off.
 */
fun reminderNotificationsAllowed(context: Context): Boolean {
    val manager = NotificationManagerCompat.from(context)
    if (!hasNotificationPermission(context) || !manager.areNotificationsEnabled()) return false
    val channel = manager.getNotificationChannelCompat(CHANNEL_ID)
    return channel == null || channel.importance != NotificationManagerCompat.IMPORTANCE_NONE
}

/** Opens the system screen where the user can turn the app's notifications on. */
fun openNotificationSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // A device without that screen: nothing we can do.
    }
}

/**
 * Shows and removes the daily-challenge reminder notification.
 *
 * The texts are read in the app's language: Android below 13 doesn't apply
 * the per-app language to a background worker, so the language is passed in
 * (see [ChallengeReminderScheduler]).
 */
@Singleton
class ChallengeReminderNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val appIntents: AppIntents,
) {

    fun show(reminder: ChallengeReminder, languageTag: String?) {
        if (!reminderNotificationsAllowed(context)) return
        val localized = localizedContext(languageTag)
        val manager = NotificationManagerCompat.from(context)
        // Created (or renamed, after a language change) every time; cheap and idempotent.
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(localized.getString(R.string.reminder_channel_name))
                .setDescription(localized.getString(R.string.reminder_channel_description))
                .build(),
        )

        val text = ReminderTexts.pick(reminder)
        val body = text.text(localized.resources)
        val open = PendingIntent.getActivity(
            context,
            0,
            appIntents.openDailyChallenge(context),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(BRAND_YELLOW)
            .setContentTitle(localized.getString(text.title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        // Checked again right here for lint; reminderNotificationsAllowed covers it too.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        manager.notify(NOTIFICATION_ID, notification)
    }

    /** Removes a shown reminder (the app was opened, so it's out of date). */
    fun cancel() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    // Every install has both languages: :app turns off the bundle's language split.
    @SuppressLint("AppBundleLocaleChanges")
    private fun localizedContext(languageTag: String?): Context {
        if (languageTag == null) return context
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(languageTag))
        return context.createConfigurationContext(configuration)
    }
}
