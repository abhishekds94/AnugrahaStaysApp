package com.anugraha.stays.presentation.screens.booking_details

import com.anugraha.stays.util.ViewIntent

sealed class BookingDetailsIntent : ViewIntent {
    data class LoadBooking(val reservationId: Int) : BookingDetailsIntent()
    data class OpenWhatsApp(val phoneNumber: String) : BookingDetailsIntent()
}