package com.m7mdabaza.xogame.presentation.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.m7mdabaza.xogame.domain.model.ReminderSchedule
import com.m7mdabaza.xogame.domain.repository.TimeProvider
import com.m7mdabaza.xogame.domain.usecase.OnChallengeReminderDueUseCase
import com.m7mdabaza.xogame.presentation.settings.AppLanguageController
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules the daily-challenge reminder with WorkManager (see
 * [ReminderSchedule]): 24 hours after the app was last opened, then every
 * 24 hours while it stays closed. One unique piece of work holds the next
 * reminder; [ChallengeReminderWorker] decides when it runs whether to show
 * anything (reminder setting, today's puzzle solved).
 */
@Singleton
class ChallengeReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val time: TimeProvider,
    private val language: AppLanguageController,
    private val notifier: ChallengeReminderNotifier,
) {

    /** Call whenever the app comes to the foreground: the countdown starts again, and a shown reminder is out of date. */
    fun onAppOpened() {
        notifier.cancel()
        enqueue(reminderNumber = 1, languageTag = language.current.tag, policy = ExistingWorkPolicy.REPLACE)
    }

    /** From the running worker: the next reminder, 24 hours later. Runs once the current one has finished. */
    internal fun scheduleNext(reminderNumber: Int, languageTag: String?) {
        enqueue(reminderNumber, languageTag, ExistingWorkPolicy.APPEND_OR_REPLACE)
    }

    private fun enqueue(reminderNumber: Int, languageTag: String?, policy: ExistingWorkPolicy) {
        val now = time.nowMillis()
        val request = OneTimeWorkRequestBuilder<ChallengeReminderWorker>()
            .setInitialDelay(ReminderSchedule.nextAt(now) - now, TimeUnit.MILLISECONDS)
            // The language travels with the work: below Android 13 a worker can't read the per-app language.
            .setInputData(workDataOf(KEY_NUMBER to reminderNumber, KEY_LANGUAGE to languageTag))
            .addTag(WORK_NAME)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, policy, request)
    }

    internal companion object {
        const val WORK_NAME = "daily_challenge_reminder"
        const val KEY_NUMBER = "reminder_number"
        const val KEY_LANGUAGE = "language_tag"
    }
}

/** What [ChallengeReminderWorker] needs from Hilt (WorkManager creates workers itself). */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ChallengeReminderEntryPoint {
    fun onReminderDue(): OnChallengeReminderDueUseCase
    fun notifier(): ChallengeReminderNotifier
    fun scheduler(): ChallengeReminderScheduler
}

/** Runs when a reminder is due: shows it if today's puzzle is still open, and schedules the next one. */
class ChallengeReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, ChallengeReminderEntryPoint::class.java)
        val number = inputData.getInt(ChallengeReminderScheduler.KEY_NUMBER, 1)
        val language = inputData.getString(ChallengeReminderScheduler.KEY_LANGUAGE)
        val due = deps.onReminderDue()(number)
        due.show?.let { deps.notifier().show(it, language) }
        if (due.scheduleNext) deps.scheduler().scheduleNext(number + 1, language)
        return Result.success()
    }
}
