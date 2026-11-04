package com.m7mdabaza.xogame.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.m7mdabaza.xogame.data.ads.AdPolicyRepositoryImpl
import com.m7mdabaza.xogame.data.ads.SystemTimeProvider
import com.m7mdabaza.xogame.data.billing.BillingGateway
import com.m7mdabaza.xogame.data.billing.PlayBillingGateway
import com.m7mdabaza.xogame.data.billing.PurchaseRepositoryImpl
import com.m7mdabaza.xogame.data.settings.SettingsRepositoryImpl
import com.m7mdabaza.xogame.data.stats.StatsRepositoryImpl
import com.m7mdabaza.xogame.domain.repository.AdPolicyRepository
import com.m7mdabaza.xogame.domain.repository.PurchaseRepository
import com.m7mdabaza.xogame.domain.repository.SettingsRepository
import com.m7mdabaza.xogame.domain.repository.StatsRepository
import com.m7mdabaza.xogame.domain.repository.TimeProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SettingsStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class StatsStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AdPolicyStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PurchaseStore

/** Binds the :domain repository interfaces to their :data implementations. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {

    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    abstract fun bindStatsRepository(impl: StatsRepositoryImpl): StatsRepository

    @Binds
    abstract fun bindAdPolicyRepository(impl: AdPolicyRepositoryImpl): AdPolicyRepository

    @Binds
    abstract fun bindPurchaseRepository(impl: PurchaseRepositoryImpl): PurchaseRepository

    @Binds
    abstract fun bindBillingGateway(impl: PlayBillingGateway): BillingGateway

    @Binds
    abstract fun bindTimeProvider(impl: SystemTimeProvider): TimeProvider

    companion object {
        // DataStore allows only one instance per file in a process, hence @Singleton.
        @Provides
        @Singleton
        @SettingsStore
        fun provideSettingsStore(@ApplicationContext context: Context): DataStore<Preferences> = store(context, "settings")

        @Provides
        @Singleton
        @StatsStore
        fun provideStatsStore(@ApplicationContext context: Context): DataStore<Preferences> = store(context, "stats")

        @Provides
        @Singleton
        @AdPolicyStore
        fun provideAdPolicyStore(@ApplicationContext context: Context): DataStore<Preferences> = store(context, "ad_policy")

        @Provides
        @Singleton
        @PurchaseStore
        fun providePurchaseStore(@ApplicationContext context: Context): DataStore<Preferences> = store(context, "purchases")

        private fun store(context: Context, name: String): DataStore<Preferences> =
            PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(name) }
    }
}
