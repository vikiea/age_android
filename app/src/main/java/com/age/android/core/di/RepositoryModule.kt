package com.age.android.core.di

import com.age.android.core.age.AgeEngine
import com.age.engine.AgeEngineImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideAgeEngine(): AgeEngine = AgeEngineImpl()
}
