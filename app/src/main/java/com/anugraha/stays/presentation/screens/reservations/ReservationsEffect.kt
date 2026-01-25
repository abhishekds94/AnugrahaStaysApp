package com.anugraha.stays.presentation.screens.reservations

import com.anugraha.stays.util.ViewEffect

sealed class ReservationsEffect : ViewEffect {
    data class ShowError(val message: String) : ReservationsEffect()
}
