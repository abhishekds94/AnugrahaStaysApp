package com.anugraha.stays.presentation.screens.new_booking

import com.anugraha.stays.util.ViewState
import java.time.LocalDate

data class NewBookingState(
    val guestName: String = "",
    val guestEmail: String = "",
    val contactNumber: String = "",
    val checkInDate: LocalDate = LocalDate.now(),
    val checkOutDate: LocalDate = LocalDate.now().plusDays(1),
    val arrivalTime: String = "",
    val guestsCount: Int = 1,
    val hasPet: Boolean = false,
    val roomId: Int = 1,
    val amountPaid: String = "",
    val transactionId: String = "",
    val bookingSource: String = "DIRECT",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
) : ViewState
