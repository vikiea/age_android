package com.age.android.core.di

import android.content.Context
import androidx.room.Room
import com.age.android.core.data.AppDatabase
import com.age.android.core.data.KeyDao
import com.age.android.core.data.MIGRATION_1_2
import com.age.android.core.data.OperationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "age_android.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideKeyDao(db: AppDatabase): KeyDao = db.keyDao()

    @Provides
    fun provideOperationDao(db: AppDatabase): OperationDao = db.operationDao()
}
