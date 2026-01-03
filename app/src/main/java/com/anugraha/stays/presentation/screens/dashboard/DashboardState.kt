package com.anugraha.stays.presentation.screens.dashboard

import com.anugraha.stays.domain.model.CheckIn
import com.anugraha.stays.domain.model.CheckOut
import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.domain.model.WeekBooking

data class DashboardState(
    val todayCheckIns: List<CheckIn> = emptyList(),
    val todayCheckOuts: List<CheckOut> = emptyList(),
    val weekBookings: List<WeekBooking> = emptyList(),
    val pendingReservations: List<Reservation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false
)