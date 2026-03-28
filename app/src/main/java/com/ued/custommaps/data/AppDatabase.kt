package com.ued.custommaps.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        JourneyEntity::class,
        TrackPointEntity::class,
        StopPointEntity::class,
        StopPointMediaEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun journeyDao(): JourneyDao
}