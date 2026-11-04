package com.m7mdabaza.xogame.presentation.di

import com.m7mdabaza.xogame.presentation.feedback.AndroidGameFeedback
import com.m7mdabaza.xogame.presentation.game.GameFeedback
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import kotlin.random.Random

/** The dispatcher for CPU-bound work such as the AI search. Tests replace it. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Module
@InstallIn(SingletonComponent::class)
abstract class PresentationModule {

    @Binds
    abstract fun bindGameFeedback(impl: AndroidGameFeedback): GameFeedback

    companion object {
        @Provides
        @DefaultDispatcher
        fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

        @Provides
        fun provideRandom(): Random = Random.Default
    }
}
