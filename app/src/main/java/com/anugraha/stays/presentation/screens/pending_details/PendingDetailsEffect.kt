package com.anugraha.stays.presentation.screens.pending_details

import com.anugraha.stays.util.ViewEffect

sealed class PendingDetailsEffect : ViewEffect {
    object NavigateBack : PendingDetailsEffect()
    data class ShowError(val message: String) : PendingDetailsEffect()
    data class ShowToast(val message: String) : PendingDetailsEffect()
    data class OpenWhatsApp(val phoneNumber: String, val message: String) : PendingDetailsEffect()
}