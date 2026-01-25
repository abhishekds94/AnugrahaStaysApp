package com.anugraha.stays.domain.model

import java.time.LocalDate
import java.time.LocalTime

data class Reservation(
    val id: Int,
    val reservationNumber: String,
    val status: ReservationStatus,
    val checkInDate: LocalDate,
    val checkOutDate: LocalDate,
    val adults: Int,
    val kids: Int,
    val hasPet: Boolean,
    val totalAmount: Double,
    val primaryGuest: Guest,
    val room: Room?,
    val bookingSource: BookingSource,
    val estimatedCheckInTime: LocalTime? = null,
    val transactionId: String? = null,
    val paymentStatus: String? = null,
    val transportService: String? = null,
    val paymentReference: String? = null
)