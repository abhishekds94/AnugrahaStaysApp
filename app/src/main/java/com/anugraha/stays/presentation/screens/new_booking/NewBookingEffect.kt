package com.anugraha.stays.presentation.screens.new_booking

import com.anugraha.stays.util.ViewEffect

sealed class NewBookingEffect : ViewEffect {
    object NavigateBack : NewBookingEffect()
    data class ShowError(val message: String) : NewBookingEffect()
}
