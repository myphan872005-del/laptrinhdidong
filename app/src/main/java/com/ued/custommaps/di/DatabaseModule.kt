package com.ued.custommaps.di

import android.content.Context
import androidx.room.Room
import com.ued.custommaps.data.AppDatabase
import com.ued.custommaps.data.JourneyDao
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "geo_db")
            .fallbackToDestructiveMigration() // FIX: Xóa dữ liệu cũ để tạo bảng mới theo version 2
            .build()
    }

    @Provides
    fun provideJourneyDao(db: AppDatabase): JourneyDao = db.journeyDao()
}