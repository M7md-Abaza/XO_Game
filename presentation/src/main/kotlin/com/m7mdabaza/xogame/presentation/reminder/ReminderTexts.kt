package com.m7mdabaza.xogame.presentation.reminder

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.m7mdabaza.xogame.domain.model.ChallengeReminder
import com.m7mdabaza.xogame.presentation.R

/** A reminder's title and text, as resources. */
sealed interface ReminderText {
    @get:StringRes
    val title: Int

    fun text(resources: Resources): String

    data class Plain(@param:StringRes override val title: Int, @param:StringRes val textRes: Int) : ReminderText {
        override fun text(resources: Resources): String = resources.getString(textRes)
    }

    /** The text names the streak: "%d days in a row…". */
    data class Streak(@param:StringRes override val title: Int, @param:PluralsRes val textRes: Int, val days: Int) : ReminderText {
        override fun text(resources: Resources): String = resources.getQuantityString(textRes, days, days)
    }
}

/**
 * The daily-challenge reminder texts, so players don't see the same one every
 * day. Each set rotates by date, so two days in a row never repeat:
 *
 * - [streak] when the player has a streak of at least [MIN_STREAK] days that
 *   today's puzzle would extend (it ends at midnight otherwise),
 * - [comeback] from the [COMEBACK_FROM]th reminder without opening the app,
 * - [daily] otherwise.
 *
 * To add a text, add its strings (English and Arabic) and a line here.
 */
object ReminderTexts {
    const val MIN_STREAK = 2
    const val COMEBACK_FROM = 3

    val daily: List<Pair<Int, Int>> = listOf(
        R.string.reminder_daily_1_title to R.string.reminder_daily_1_text,
        R.string.reminder_daily_2_title to R.string.reminder_daily_2_text,
        R.string.reminder_daily_3_title to R.string.reminder_daily_3_text,
        R.string.reminder_daily_4_title to R.string.reminder_daily_4_text,
        R.string.reminder_daily_5_title to R.string.reminder_daily_5_text,
        R.string.reminder_daily_6_title to R.string.reminder_daily_6_text,
        R.string.reminder_daily_7_title to R.string.reminder_daily_7_text,
        R.string.reminder_daily_8_title to R.string.reminder_daily_8_text,
        R.string.reminder_daily_9_title to R.string.reminder_daily_9_text,
        R.string.reminder_daily_10_title to R.string.reminder_daily_10_text,
        R.string.reminder_daily_11_title to R.string.reminder_daily_11_text,
        R.string.reminder_daily_12_title to R.string.reminder_daily_12_text,
        R.string.reminder_daily_13_title to R.string.reminder_daily_13_text,
        R.string.reminder_daily_14_title to R.string.reminder_daily_14_text,
        R.string.reminder_daily_15_title to R.string.reminder_daily_15_text,
        R.string.reminder_daily_16_title to R.string.reminder_daily_16_text,
    )

    /** Titles (strings) with texts (plurals of the streak length). */
    val streak: List<Pair<Int, Int>> = listOf(
        R.string.reminder_streak_1_title to R.plurals.reminder_streak_1_text,
        R.string.reminder_streak_2_title to R.plurals.reminder_streak_2_text,
        R.string.reminder_streak_3_title to R.plurals.reminder_streak_3_text,
        R.string.reminder_streak_4_title to R.plurals.reminder_streak_4_text,
        R.string.reminder_streak_5_title to R.plurals.reminder_streak_5_text,
        R.string.reminder_streak_6_title to R.plurals.reminder_streak_6_text,
        R.string.reminder_streak_7_title to R.plurals.reminder_streak_7_text,
        R.string.reminder_streak_8_title to R.plurals.reminder_streak_8_text,
    )

    val comeback: List<Pair<Int, Int>> = listOf(
        R.string.reminder_comeback_1_title to R.string.reminder_comeback_1_text,
        R.string.reminder_comeback_2_title to R.string.reminder_comeback_2_text,
        R.string.reminder_comeback_3_title to R.string.reminder_comeback_3_text,
        R.string.reminder_comeback_4_title to R.string.reminder_comeback_4_text,
    )

    fun pick(reminder: ChallengeReminder): ReminderText {
        fun List<Pair<Int, Int>>.forToday() = this[reminder.epochDay.mod(size)]
        return when {
            reminder.streak >= MIN_STREAK ->
                streak.forToday().let { (title, text) -> ReminderText.Streak(title, text, reminder.streak) }
            reminder.reminderNumber >= COMEBACK_FROM ->
                comeback.forToday().let { (title, text) -> ReminderText.Plain(title, text) }
            else ->
                daily.forToday().let { (title, text) -> ReminderText.Plain(title, text) }
        }
    }
}
