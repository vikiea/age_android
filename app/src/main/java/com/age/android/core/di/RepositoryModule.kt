package com.age.android.core.di

import com.age.android.core.age.AgeEngine
import com.age.engine.AgeEngineImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAgeEngine(impl: AgeEngineImpl): AgeEngine
}
