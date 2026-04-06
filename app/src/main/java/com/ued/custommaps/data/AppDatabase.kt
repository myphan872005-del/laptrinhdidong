package com.ued.custommaps.data

import androidx.room.Database
import androidx.room.RoomDatabase

// Version 2 vì chúng ta đã thêm bảng StopPoint cho Phase 1
@Database(
    entities = [
        JourneyEntity::class,
        TrackPointEntity::class,
        StopPointEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun journeyDao(): JourneyDao
}