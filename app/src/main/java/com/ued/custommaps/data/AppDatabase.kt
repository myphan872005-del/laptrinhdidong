package com.ued.custommaps.data

import androidx.room.Database
import androidx.room.RoomDatabase


// Version 2 vì chúng ta đã thêm bảng StopPoint cho Phase 1

@Database(
    entities = [
        JourneyEntity::class,
        TrackPointEntity::class,

        StopPointEntity::class,
        StopPointMediaEntity::class,
        DiscoveryJourneyEntity::class,
        DiscoveryTrackPointEntity::class,
        DiscoveryStopPointEntity::class,
        DiscoveryMediaEntity::class

    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun trackPointDao(): TrackPointDao

    abstract fun journeyDao(): JourneyDao

    abstract fun discoveryDao(): DiscoveryDao

}