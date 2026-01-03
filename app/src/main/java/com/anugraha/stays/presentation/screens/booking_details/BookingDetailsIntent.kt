package com.anugraha.stays.presentation.screens.booking_details

sealed class BookingDetailsIntent {
    data class LoadBooking(val reservationId: Int) : BookingDetailsIntent()
}