package com.anugraha.stays.data.local.database.dao

import androidx.room.*
import com.anugraha.stays.data.local.database.entity.ExternalBookingEntity

@Dao
interface ExternalBookingDao {

    @Query("SELECT * FROM external_bookings ORDER BY check_in_date DESC")
    suspend fun getAllBookings(): List<ExternalBookingEntity>

    @Query("SELECT * FROM external_bookings WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): ExternalBookingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bookings: List<ExternalBookingEntity>)

    @Query("DELETE FROM external_bookings WHERE source = :source")
    suspend fun deleteBySource(source: String)

    @Query("DELETE FROM external_bookings")
    suspend fun deleteAll()
}