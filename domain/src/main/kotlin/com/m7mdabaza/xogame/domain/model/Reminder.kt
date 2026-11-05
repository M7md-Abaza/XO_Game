package com.m7mdabaza.xogame.domain.model

import java.util.Calendar
import java.util.TimeZone

/**
 * A daily-challenge reminder to show: today's puzzle is still unsolved.
 *
 * @property epochDay today, as days since 1970-01-01 (the texts rotate by it).
 * @property streak solved days in a row up to yesterday, which today's puzzle
 *   would extend.
 * @property reminderNumber 1 for the first reminder since the app was last
 *   opened, 2 for the next day's, and so on.
 */
data class ChallengeReminder(val epochDay: Long, val streak: Int, val reminderNumber: Int)

/**
 * When the daily-challenge reminder fires (user decision, 2026-11-05): 24 hours
 * after the app was last opened, then every 24 hours while it stays closed.
 * Opening the app starts the count again.
 *
 * - Never at night: a time from [QUIET_START_HOUR] to [QUIET_END_HOUR] moves
 *   to [QUIET_END_HOUR] in the morning.
 * - At most [MAX_IN_A_ROW] reminders while the app stays closed, so a player
 *   who stopped playing isn't nagged forever.
 */
object ReminderSchedule {
    const val INTERVAL_MILLIS = 24 * 60 * 60 * 1000L
    const val QUIET_START_HOUR = 22
    const val QUIET_END_HOUR = 9
    const val MAX_IN_A_ROW = 7

    /** When the reminder after [fromMillis] (the app was opened, or the last reminder fired) is due. */
    fun nextAt(fromMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): Long {
        val time = Calendar.getInstance(timeZone).apply { timeInMillis = fromMillis + INTERVAL_MILLIS }
        val hour = time.get(Calendar.HOUR_OF_DAY)
        if (hour in QUIET_END_HOUR until QUIET_START_HOUR) return time.timeInMillis
        // Late evening: the next morning. After midnight: this morning.
        if (hour >= QUIET_START_HOUR) time.add(Calendar.DAY_OF_YEAR, 1)
        time.set(Calendar.HOUR_OF_DAY, QUIET_END_HOUR)
        time.set(Calendar.MINUTE, 0)
        time.set(Calendar.SECOND, 0)
        time.set(Calendar.MILLISECOND, 0)
        return time.timeInMillis
    }
}
