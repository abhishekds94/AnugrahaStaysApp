package com.anugraha.stays.presentation.screens.booking_details

import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.util.ViewState

data class BookingDetailsState(
    val reservation: Reservation? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) : ViewState
