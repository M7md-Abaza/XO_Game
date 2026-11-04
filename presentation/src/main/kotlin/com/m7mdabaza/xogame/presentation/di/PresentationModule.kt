package com.m7mdabaza.xogame.presentation.di

import com.m7mdabaza.xogame.presentation.feedback.AndroidGameFeedback
import com.m7mdabaza.xogame.presentation.game.GameFeedback
import com.m7mdabaza.xogame.presentation.settings.AndroidAppLanguageController
import com.m7mdabaza.xogame.presentation.settings.AppLanguageController
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlin.random.Random

/** The dispatcher for CPU-bound work such as the AI search. Tests replace it. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/** A scope that lives as long as the app, for app-wide observers. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
abstract class PresentationModule {

    @Binds
    abstract fun bindGameFeedback(impl: AndroidGameFeedback): GameFeedback

    @Binds
    abstract fun bindAppLanguageController(impl: AndroidAppLanguageController): AppLanguageController

    companion object {
        @Provides
        @DefaultDispatcher
        fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        @Provides
        fun provideRandom(): Random = Random.Default
    }
}
