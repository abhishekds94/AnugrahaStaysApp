package com.anugraha.stays.domain.usecase.booking

import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.domain.repository.BookingRepository
import com.anugraha.stays.util.NetworkResult
import javax.inject.Inject

class CreateBookingUseCase @Inject constructor(
    private val bookingRepository: BookingRepository
) {
    suspend operator fun invoke(
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
    ): NetworkResult<Reservation> {
        // Validation
        if (guestName.isBlank()) {
            return NetworkResult.Error("Guest name is required")
        }
        if (contactNumber.isBlank()) {
            return NetworkResult.Error("Contact number is required")
        }
        if (guestsCount < 1) {
            return NetworkResult.Error("At least one guest is required")
        }

        return bookingRepository.createAdminBooking(
            guestName = guestName,
            guestEmail = guestEmail,
            contactNumber = contactNumber,
            checkInDate = checkInDate,
            checkOutDate = checkOutDate,
            arrivalTime = arrivalTime,
            guestsCount = guestsCount,
            isPet = isPet,
            roomId = roomId,
            amountPaid = amountPaid,
            transactionId = transactionId,
            bookingSource = bookingSource
        )
    }
}