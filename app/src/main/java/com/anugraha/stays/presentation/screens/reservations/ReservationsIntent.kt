package com.anugraha.stays.presentation.screens.reservations

import java.time.YearMonth

sealed class ReservationsIntent {
    object LoadReservations : ReservationsIntent()
    data class SearchQueryChanged(val query: String) : ReservationsIntent()
    data class ToggleMonthExpansion(val month: YearMonth) : ReservationsIntent()
}