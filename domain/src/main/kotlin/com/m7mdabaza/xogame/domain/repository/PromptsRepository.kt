package com.m7mdabaza.xogame.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * When the app last asked for a Play review, offered an app update, or asked
 * for the notification permission (epoch millis, null = never).
 */
interface PromptsRepository {
    val reviewRequestedAt: Flow<Long?>
    val updatePromptedAt: Flow<Long?>
    val notificationPermissionAskedAt: Flow<Long?>

    suspend fun setReviewRequestedAt(millis: Long)
    suspend fun setUpdatePromptedAt(millis: Long)
    suspend fun setNotificationPermissionAskedAt(millis: Long)
}
