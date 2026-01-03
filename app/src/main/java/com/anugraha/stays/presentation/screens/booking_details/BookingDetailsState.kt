package com.anugraha.stays.presentation.screens.booking_details

import com.anugraha.stays.domain.model.Reservation

data class BookingDetailsState(
    val reservation: Reservation? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)