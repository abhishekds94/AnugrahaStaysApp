package com.anugraha.stays.presentation.screens.booking_details

import com.anugraha.stays.util.ViewEffect

sealed class BookingDetailsEffect : ViewEffect {
    data class ShowError(val message: String) : BookingDetailsEffect()
    data class ShowToast(val message: String) : BookingDetailsEffect()
    data class OpenWhatsApp(val phoneNumber: String) : BookingDetailsEffect()
    object ShowImageSourceDialog : BookingDetailsEffect()
    data class OpenImageViewer(val imageUrl: String) : BookingDetailsEffect()
}