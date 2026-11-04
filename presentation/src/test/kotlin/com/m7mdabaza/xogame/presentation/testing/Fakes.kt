package com.m7mdabaza.xogame.presentation.testing

import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.Level
import com.m7mdabaza.xogame.domain.model.LevelStats
import com.m7mdabaza.xogame.domain.model.Settings
import com.m7mdabaza.xogame.domain.model.Stats
import com.m7mdabaza.xogame.domain.repository.SettingsRepository
import com.m7mdabaza.xogame.domain.repository.StatsRepository
import com.m7mdabaza.xogame.presentation.settings.AppLanguage
import com.m7mdabaza.xogame.presentation.settings.AppLanguageController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeSettingsRepository(initial: Settings = Settings()) : SettingsRepository {
    override val settings = MutableStateFlow(initial)

    override suspend fun update(transform: (Settings) -> Settings) {
        settings.update(transform)
    }
}

class FakeStatsRepository(initial: Stats = Stats()) : StatsRepository {
    override val stats = MutableStateFlow(initial)

    override suspend fun update(mode: GameMode, level: Level, transform: (LevelStats) -> LevelStats) {
        stats.update { Stats(it.levels + ((mode to level) to transform(it[mode, level]))) }
    }

    override suspend fun reset() {
        stats.value = Stats()
    }
}

class FakeLanguageController(override var current: AppLanguage = AppLanguage.SYSTEM) : AppLanguageController {
    val applied = mutableListOf<AppLanguage>()

    override fun set(language: AppLanguage) {
        applied += language
        current = language
    }
}
