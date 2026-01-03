package com.anugraha.stays.domain.repository

import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.util.NetworkResult

interface BookingRepository {
    suspend fun createAdminBooking(
        guestName: String,
        guestEmail: String?,
        contactNumber: String,
        checkInDate: String,
        checkOutDate: String,
        arrivalTime: String?,
        guestsCount: Int,
        isPet: Boolean,
        roomId: Int,
        amountPaid: Double?,
        transactionId: String?,
        bookingSource: String
    ): NetworkResult<Reservation>
}