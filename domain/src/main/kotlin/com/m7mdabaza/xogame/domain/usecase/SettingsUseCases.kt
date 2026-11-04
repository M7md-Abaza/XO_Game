package com.m7mdabaza.xogame.domain.usecase

import com.m7mdabaza.xogame.domain.model.Settings
import com.m7mdabaza.xogame.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<Settings> = repository.settings
}

class UpdateSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(transform: (Settings) -> Settings) = repository.update(transform)
}
