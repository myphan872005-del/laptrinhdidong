package com.ued.custommaps.di

import android.content.Context
import androidx.room.Room
import com.ued.custommaps.data.AppDatabase
import com.ued.custommaps.data.DiscoveryDao
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
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "geo_db"
        )
            .fallbackToDestructiveMigration() // Tự dọn dẹp data cũ nếu đổi version để tránh Crash
            .build()
    }

    @Provides
    @Singleton
    fun provideJourneyDao(db: AppDatabase): JourneyDao {
        return db.journeyDao()
    }

    @Provides
    @Singleton
    fun provideDiscoveryDao(db: AppDatabase): DiscoveryDao {
        return db.discoveryDao()
    }
}