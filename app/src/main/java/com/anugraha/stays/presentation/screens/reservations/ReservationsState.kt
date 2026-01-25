package com.anugraha.stays.presentation.screens.reservations

import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.util.ViewState
import java.time.YearMonth

data class ReservationsState(
    val reservations: List<Reservation> = emptyList(),
    val filteredReservations: List<Reservation> = emptyList(),
    val groupedReservations: Map<YearMonth, List<Reservation>> = emptyMap(),
    val expandedMonths: Set<YearMonth> = emptySet(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isLoadingExternal: Boolean = false,
    val error: String? = null
) : ViewState

fun ReservationsState.getSortedMonths(): List<YearMonth> {
    return groupedReservations.keys.sortedDescending()
}
