package com.anugraha.stays.presentation.screens.reservations

sealed class ReservationsIntent {
    object LoadReservations : ReservationsIntent()
    data class SearchQueryChanged(val query: String) : ReservationsIntent()
}