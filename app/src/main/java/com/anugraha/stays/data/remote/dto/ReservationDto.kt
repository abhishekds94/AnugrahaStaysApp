package com.anugraha.stays.data.remote.dto

import android.util.Log
import com.anugraha.stays.domain.model.BookingSource
import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.domain.model.ReservationStatus
import com.anugraha.stays.domain.model.Guest
import com.anugraha.stays.domain.model.Room
import com.anugraha.stays.util.DateUtils
import com.google.gson.annotations.SerializedName
import java.time.LocalDate
import java.time.LocalTime

data class ReservationDto(
    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("reservation_number")
    val reservationNumber: String? = null,

    @SerializedName("status")
    val status: String? = null,

    @SerializedName("check_in_date")
    val checkInDate: String? = null,

    @SerializedName("check_out_date")
    val checkOutDate: String? = null,

    @SerializedName("adults")
    val adults: Int? = null,

    @SerializedName("kids")
    val kids: Int? = null,

    @SerializedName("pets")
    val pets: Int? = null,

    @SerializedName("room_id")
    val roomId: Int? = null,

    @SerializedName("ac_room")
    val acRoom: Int? = null,

    @SerializedName("total_amount")
    val totalAmount: String? = null,

    @SerializedName("payment_reference")
    val paymentReference: String? = null,

    @SerializedName("payment_amount")
    val paymentAmount: String? = null,

    @SerializedName("primary_guest_id")
    val primaryGuestId: Int? = null,

    @SerializedName("additional_requests")
    val additionalRequests: String? = null,

    @SerializedName("transport_service")
    val transportService: String? = null,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("updated_at")
    val updatedAt: String? = null,

    // Nested objects from API responses
    @SerializedName("primary_guest")
    val primaryGuest: GuestDto? = null,

    @SerializedName("guest")
    val guest: GuestDto? = null,

    @SerializedName("room")
    val room: RoomDto? = null,

    @SerializedName("booking_source")
    val bookingSource: String? = null,

    @SerializedName("estimated_check_in_time")
    val estimatedCheckInTime: String? = null,

    @SerializedName("arrival_time")
    val arrivalTime: String? = null
)

fun ReservationDto.toDomain(): Reservation? {
    // Validate required fields
    if (id == null || checkInDate == null || checkOutDate == null) {
        Log.e("ReservationDto", "Missing required fields: id=$id, checkIn=$checkInDate, checkOut=$checkOutDate")
        return null
    }

    return try {
        // PRIORITY 1: Use nested primaryGuest object if available
        val guestDto = primaryGuest ?: guest

        val guest = if (guestDto != null) {
            Log.d("ReservationDto", "Using guest data: ${guestDto.fullName}")
            guestDto.toDomain()
        } else {
            Log.w("ReservationDto", "No guest data found, using fallback for ID: $id")
            Guest(
                fullName = "Guest #${primaryGuestId ?: id}",
                phone = "Not Available",
                email = null
            )
        }

        // Create room object
        val roomObj = if (room != null) {
            room.toDomain()
        } else {
            Room(
                id = roomId ?: 0,
                title = when (acRoom) {
                    1 -> "A/C Room"
                    0 -> "Non A/C Room"
                    else -> "Standard Room"
                }
            )
        }

        // Parse check-in time
        val checkInTime = (estimatedCheckInTime ?: arrivalTime)?.let {
            parseTime(it)
        }

        val reservation = Reservation(
            id = id,
            reservationNumber = reservationNumber ?: "RES-$id",
            status = ReservationStatus.fromString(status ?: "pending"),
            checkInDate = parseDate(checkInDate),
            checkOutDate = parseDate(checkOutDate),
            adults = adults ?: 1,
            kids = kids ?: 0,
            hasPet = (pets ?: 0) > 0,
            totalAmount = totalAmount?.toDoubleOrNull() ?: 0.0,
            primaryGuest = guest,
            room = roomObj,
            bookingSource = BookingSource.fromString(bookingSource ?: "direct"),
            estimatedCheckInTime = checkInTime,
            transactionId = paymentReference,
            paymentStatus = if ((paymentAmount?.toDoubleOrNull() ?: 0.0) > 0) "Paid" else "Pending"
        )

        Log.d("ReservationDto", "Successfully mapped reservation ${reservation.reservationNumber} with guest: ${reservation.primaryGuest.fullName}")

        reservation
    } catch (e: Exception) {
        Log.e("ReservationDto", "Error converting DTO to Domain: ${e.message}", e)
        null
    }
}

private fun parseDate(dateString: String): LocalDate {
    return try {
        DateUtils.parseDate(dateString)
    } catch (e: Exception) {
        Log.e("ReservationDto", "Error parsing date: $dateString", e)
        LocalDate.now()
    }
}

private fun parseTime(timeString: String): LocalTime? {
    return try {
        // Handle different time formats
        when {
            timeString.contains(":") && timeString.length <= 8 -> {
                // Format: "11:00:00" or "11:00"
                LocalTime.parse(timeString)
            }
            timeString.contains(":") -> {
                // Format with timezone: "11:00:00.000000Z"
                LocalTime.parse(timeString.substringBefore("."))
            }
            else -> null
        }
    } catch (e: Exception) {
        Log.e("ReservationDto", "Error parsing time: $timeString", e)
        null
    }
}