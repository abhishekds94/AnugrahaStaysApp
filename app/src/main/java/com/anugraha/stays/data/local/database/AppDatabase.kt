package com.anugraha.stays.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.anugraha.stays.data.local.database.dao.ExternalBookingDao
import com.anugraha.stays.data.local.database.entity.ExternalBookingEntity

@Database(
    entities = [
        ExternalBookingEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun externalBookingDao(): ExternalBookingDao
}