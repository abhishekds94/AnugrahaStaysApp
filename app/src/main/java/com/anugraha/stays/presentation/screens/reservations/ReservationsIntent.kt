package com.anugraha.stays.presentation.screens.reservations

import com.anugraha.stays.util.ViewIntent
import java.time.YearMonth

sealed class ReservationsIntent : ViewIntent {
    object LoadReservations : ReservationsIntent()
    data class SearchQueryChanged(val query: String) : ReservationsIntent()
    data class ToggleMonthExpansion(val month: YearMonth) : ReservationsIntent()
}
