package com.anugraha.stays.presentation.screens.reservations

import com.anugraha.stays.domain.model.Reservation
import java.time.YearMonth

data class ReservationsState(
    val reservations: List<Reservation> = emptyList(),
    val filteredReservations: List<Reservation> = emptyList(),
    val groupedReservations: Map<YearMonth, List<Reservation>> = emptyMap(),
    val expandedMonths: Set<YearMonth> = emptySet(), // Track which months are expanded
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isLoadingExternal: Boolean = false,
    val error: String? = null
)


fun ReservationsState.getSortedMonths(): List<YearMonth> {
    return groupedReservations.keys.sortedDescending()
}