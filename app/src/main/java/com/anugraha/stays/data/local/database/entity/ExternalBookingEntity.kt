package com.anugraha.stays.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.anugraha.stays.domain.model.*
import java.time.LocalDate

@Entity(tableName = "external_bookings")
data class ExternalBookingEntity(
    @PrimaryKey
    val uid: String,

    @ColumnInfo(name = "reservation_number")
    val reservationNumber: String,

    @ColumnInfo(name = "source")
    val source: String,

    @ColumnInfo(name = "summary")
    val summary: String,

    @ColumnInfo(name = "check_in_date")
    val checkInDate: String,

    @ColumnInfo(name = "check_out_date")
    val checkOutDate: String,

    @ColumnInfo(name = "synced_at")
    val syncedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Reservation {
        return Reservation(
            id = uid.hashCode(),
            reservationNumber = reservationNumber,
            status = ReservationStatus.APPROVED,
            checkInDate = LocalDate.parse(checkInDate),
            checkOutDate = LocalDate.parse(checkOutDate),
            adults = 2,
            kids = 0,
            hasPet = false,
            totalAmount = 0.0,
            primaryGuest = Guest(
                fullName = summary,
                phone = "",
                email = ""
            ),
            room = null,
            bookingSource = BookingSource.fromString(source),
            estimatedCheckInTime = null,
            transactionId = null,
            paymentStatus = "N/A",
            transportService = "No",
            paymentReference = null
        )
    }
}