package com.anugraha.stays.presentation.screens.new_booking

import com.anugraha.stays.util.ViewState
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class RoomType {
    AC, NON_AC
}

data class NewBookingState(
    val guestName: String = "",
    val guestEmail: String = "",
    val contactNumber: String = "",
    val checkInDate: LocalDate = LocalDate.now(),
    val checkOutDate: LocalDate = LocalDate.now().plusDays(1),
    val arrivalTime: String = "",
    val guestsCount: Int = 0,
    val hasPet: Boolean = false,
    val numberOfPets: Int = 1,
    val roomType: RoomType = RoomType.NON_AC,
    val numberOfAcRooms: Int = 1,
    val amountPaid: String = "",
    val transactionId: String = "",
    val bookingSource: String = "DIRECT",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
) : ViewState {

    // Calculate total amount based on booking details
    fun calculateTotalAmount(): Double {
        val numberOfNights = ChronoUnit.DAYS.between(checkInDate, checkOutDate).toInt()
        if (numberOfNights <= 0) return 0.0

        // Base price per night for up to 4 guests
        val basePrice = 2250.0

        // Room charges
        val roomCharges = when (roomType) {
            RoomType.NON_AC -> basePrice * numberOfNights
            RoomType.AC -> (basePrice + (500.0 * numberOfAcRooms)) * numberOfNights
        }

        // Extra guest charges (more than 4 guests)
        val extraGuestCharges = if (guestsCount > 4) {
            (guestsCount - 4) * 300.0 * numberOfNights
        } else {
            0.0
        }

        // Pet charges
        val petCharges = if (hasPet) {
            numberOfPets * 500.0 * numberOfNights
        } else {
            0.0
        }

        return roomCharges + extraGuestCharges + petCharges
    }
}