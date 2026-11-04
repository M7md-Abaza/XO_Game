package com.m7mdabaza.xogame.domain.repository

import com.m7mdabaza.xogame.domain.model.Settings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    /** The current settings, then every change. Defaults until the user changes something. */
    val settings: Flow<Settings>

    /** Atomically replaces the settings with [transform] applied to the current ones. */
    suspend fun update(transform: (Settings) -> Settings)
}
