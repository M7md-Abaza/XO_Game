package com.m7mdabaza.xogame.domain.usecase

import com.m7mdabaza.xogame.domain.model.ChallengeReminder
import com.m7mdabaza.xogame.domain.model.ReminderSchedule
import com.m7mdabaza.xogame.domain.model.Settings
import com.m7mdabaza.xogame.domain.repository.TimeProvider
import com.m7mdabaza.xogame.domain.testing.FakeChallengeRepository
import com.m7mdabaza.xogame.domain.testing.FakePromptsRepository
import com.m7mdabaza.xogame.domain.testing.FakeSettingsRepository
import com.m7mdabaza.xogame.domain.testing.FakeTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderUseCasesTest {

    private val settings = FakeSettingsRepository()
    private val challenges = FakeChallengeRepository()
    private val prompts = FakePromptsRepository()
    private val time = FakeTime(now = 20_000 * TimeProvider.MILLIS_PER_DAY + 12 * 60 * 60 * 1000L)
    private val today = time.todayEpochDay()
    private val onReminderDue = OnChallengeReminderDueUseCase(settings, challenges, time)
    private val shouldAsk = ShouldAskNotificationPermissionUseCase(settings, prompts)
    private val recordAsked = RecordNotificationPermissionAskedUseCase(prompts, time)

    @Test
    fun `an unsolved day shows a reminder and schedules the next`() = runTest {
        assertEquals(ReminderDue(ChallengeReminder(today, streak = 0, reminderNumber = 1), scheduleNext = true), onReminderDue(1))
    }

    @Test
    fun `the reminder carries the streak today's puzzle would extend`() = runTest {
        challenges.solvedDays.value = setOf(today - 1, today - 2, today - 3, today - 5)
        assertEquals(3, onReminderDue(1).show?.streak)
    }

    @Test
    fun `stays quiet once today's puzzle is solved, but keeps the schedule`() = runTest {
        challenges.solvedDays.value = setOf(today)
        assertEquals(ReminderDue(show = null, scheduleNext = true), onReminderDue(1))
    }

    @Test
    fun `reminders off - nothing shown and nothing scheduled`() = runTest {
        settings.settings.value = Settings(challengeReminder = false)
        assertEquals(ReminderDue(show = null, scheduleNext = false), onReminderDue(1))
    }

    @Test
    fun `stops after the maximum number of reminders in a row`() = runTest {
        val max = ReminderSchedule.MAX_IN_A_ROW
        assertTrue(onReminderDue(max - 1).scheduleNext)
        val last = onReminderDue(max)
        assertEquals(max, last.show?.reminderNumber)
        assertFalse(last.scheduleNext)
    }

    @Test
    fun `the notification permission is asked once, and only while reminders are on`() = runTest {
        assertTrue(shouldAsk())
        settings.settings.value = Settings(challengeReminder = false)
        assertFalse(shouldAsk())
        settings.settings.value = Settings()
        recordAsked()
        assertEquals(time.now, prompts.notificationPermissionAskedAt.value)
        assertFalse(shouldAsk())
    }
}
