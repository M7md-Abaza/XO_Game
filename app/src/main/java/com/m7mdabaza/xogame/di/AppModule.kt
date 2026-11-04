package com.m7mdabaza.xogame.di

import com.m7mdabaza.xogame.presentation.ads.AdUnitIds
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    abstract fun bindAdUnitIds(impl: AppAdUnitIds): AdUnitIds
}
