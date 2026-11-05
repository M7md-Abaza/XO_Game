package com.m7mdabaza.xogame.data.prompts

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import com.m7mdabaza.xogame.data.di.PromptsStore
import com.m7mdabaza.xogame.domain.repository.PromptsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PromptsRepositoryImpl @Inject constructor(
    @param:PromptsStore private val store: DataStore<Preferences>,
) : PromptsRepository {

    private val data = store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }

    override val reviewRequestedAt: Flow<Long?> = data.map { it[REVIEW] }.distinctUntilChanged()
    override val updatePromptedAt: Flow<Long?> = data.map { it[UPDATE] }.distinctUntilChanged()
    override val notificationPermissionAskedAt: Flow<Long?> = data.map { it[NOTIFICATION_PERMISSION] }.distinctUntilChanged()

    override suspend fun setReviewRequestedAt(millis: Long) {
        store.edit { it[REVIEW] = millis }
    }

    override suspend fun setUpdatePromptedAt(millis: Long) {
        store.edit { it[UPDATE] = millis }
    }

    override suspend fun setNotificationPermissionAskedAt(millis: Long) {
        store.edit { it[NOTIFICATION_PERMISSION] = millis }
    }

    private companion object {
        val REVIEW = longPreferencesKey("review_requested_at")
        val UPDATE = longPreferencesKey("update_prompted_at")
        val NOTIFICATION_PERMISSION = longPreferencesKey("notification_permission_asked_at")
    }
}
