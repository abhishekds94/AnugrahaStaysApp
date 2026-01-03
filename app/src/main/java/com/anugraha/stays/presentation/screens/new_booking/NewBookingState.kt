package com.anugraha.stays.presentation.screens.new_booking

data class NewBookingState(
    val guestName: String = "",
    val guestEmail: String = "",
    val contactNumber: String = "",
    val checkInDate: String = "",
    val checkOutDate: String = "",
    val arrivalTime: String = "",
    val guestsCount: Int = 2,
    val hasPet: Boolean = false,
    val roomId: Int = 1,
    val amountPaid: String = "",
    val transactionId: String = "",
    val bookingSource: String = "Direct",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)