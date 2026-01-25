package com.anugraha.stays.data.repository

import android.util.Log
import com.anugraha.stays.data.remote.api.AnugrahaApi
import com.anugraha.stays.data.remote.dto.CreateBookingRequest
import com.anugraha.stays.data.remote.dto.toDomain
import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.domain.repository.BookingRepository
import com.anugraha.stays.util.NetworkResult
import javax.inject.Inject

class BookingRepositoryImpl @Inject constructor(
    private val api: AnugrahaApi
) : BookingRepository {

    override suspend fun createAdminBooking(
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
        return try {
            val request = CreateBookingRequest(
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

            val response = api.createAdminBooking(request)

            if (response.isSuccessful) {
                val reservationDto = response.body()?.data
                if (reservationDto != null) {
                    val reservation = reservationDto.toDomain()
                    if (reservation != null) {
                        NetworkResult.Success(reservation)
                    } else {
                        NetworkResult.Error("Failed to parse booking response")
                    }
                } else {
                    NetworkResult.Error("Empty response from server")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("BookingRepo", "Error ${response.code()}: $errorBody")
                NetworkResult.Error("Failed to create booking: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("BookingRepo", "Exception creating booking", e)
            NetworkResult.Error(e.message ?: "An error occurred")
        }
    }
}