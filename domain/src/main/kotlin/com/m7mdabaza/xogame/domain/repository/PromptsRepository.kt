package com.m7mdabaza.xogame.domain.repository

import kotlinx.coroutines.flow.Flow

/** When the app last asked for a Play review or offered an app update (epoch millis, null = never). */
interface PromptsRepository {
    val reviewRequestedAt: Flow<Long?>
    val updatePromptedAt: Flow<Long?>

    suspend fun setReviewRequestedAt(millis: Long)
    suspend fun setUpdatePromptedAt(millis: Long)
}
