package com.m7mdabaza.xogame.domain.usecase

import com.m7mdabaza.xogame.domain.model.ChallengeProgress
import com.m7mdabaza.xogame.domain.model.ChallengeReminder
import com.m7mdabaza.xogame.domain.model.ReminderSchedule
import com.m7mdabaza.xogame.domain.repository.ChallengeRepository
import com.m7mdabaza.xogame.domain.repository.PromptsRepository
import com.m7mdabaza.xogame.domain.repository.SettingsRepository
import com.m7mdabaza.xogame.domain.repository.TimeProvider
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * What to do when a daily-challenge reminder is due.
 *
 * @property show the reminder to show, or null to stay quiet (reminders are
 *   off, or today's puzzle is already solved).
 * @property scheduleNext whether to schedule the next one, 24 hours later.
 */
data class ReminderDue(val show: ChallengeReminder?, val scheduleNext: Boolean)

/** Decides what the reminder number [reminderNumber] since the app was last opened does (see [ReminderSchedule]). */
class OnChallengeReminderDueUseCase @Inject constructor(
    private val settings: SettingsRepository,
    private val challenges: ChallengeRepository,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(reminderNumber: Int): ReminderDue {
        if (!settings.settings.first().challengeReminder) return ReminderDue(show = null, scheduleNext = false)
        val progress = ChallengeProgress(challenges.solvedDays.first(), time.todayEpochDay())
        val show = if (progress.solvedToday) null else ChallengeReminder(progress.today, progress.currentStreak, reminderNumber)
        return ReminderDue(show, scheduleNext = reminderNumber < ReminderSchedule.MAX_IN_A_ROW)
    }
}

/**
 * Whether to ask for the notification permission (Android 13+) now: once
 * ever, on Home at startup (user decision), and only while the reminder is
 * on. After that the player can still allow it from Settings.
 */
class ShouldAskNotificationPermissionUseCase @Inject constructor(
    private val settings: SettingsRepository,
    private val prompts: PromptsRepository,
) {
    suspend operator fun invoke(): Boolean =
        settings.settings.first().challengeReminder && prompts.notificationPermissionAskedAt.first() == null
}

class RecordNotificationPermissionAskedUseCase @Inject constructor(
    private val prompts: PromptsRepository,
    private val time: TimeProvider,
) {
    suspend operator fun invoke() = prompts.setNotificationPermissionAskedAt(time.nowMillis())
}
