/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.core.di

import android.content.Context
import androidx.room.Room
import io.github.vikiea.age.core.data.AppDatabase
import io.github.vikiea.age.core.data.KeyDao
import io.github.vikiea.age.core.data.MIGRATION_1_2
import io.github.vikiea.age.core.data.MIGRATION_2_3
import io.github.vikiea.age.core.data.migration3To4
import io.github.vikiea.age.core.data.OperationDao
import io.github.vikiea.age.core.security.AndroidPrivateKeyStore
import io.github.vikiea.age.core.security.PrivateKeyStore
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
    fun providePrivateKeyStore(@ApplicationContext context: Context): PrivateKeyStore =
        AndroidPrivateKeyStore(context)

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        privateKeyStore: PrivateKeyStore
    ): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "age_android.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, migration3To4(privateKeyStore))
            .build()

    @Provides
    fun provideKeyDao(db: AppDatabase): KeyDao = db.keyDao()

    @Provides
    fun provideOperationDao(db: AppDatabase): OperationDao = db.operationDao()
}
