package com.anugraha.stays.presentation.screens.reservations

import com.anugraha.stays.domain.model.Reservation

data class ReservationsState(
    val reservations: List<Reservation> = emptyList(),
    val filteredReservations: List<Reservation> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)