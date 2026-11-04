package com.m7mdabaza.xogame.data.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds the :domain repository interfaces to their :data implementations.
 * Bindings are added as each repository is implemented (milestone 5 onwards).
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule
