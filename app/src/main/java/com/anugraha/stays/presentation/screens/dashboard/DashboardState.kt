package com.anugraha.stays.presentation.screens.dashboard

import com.anugraha.stays.domain.model.CheckIn
import com.anugraha.stays.domain.model.CheckOut
import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.domain.model.WeekBooking
import com.anugraha.stays.util.ViewState

data class DashboardState(
    val todayCheckIns: List<CheckIn> = emptyList(),
    val todayCheckOuts: List<CheckOut> = emptyList(),
    val weekBookings: List<WeekBooking> = emptyList(),
    val pendingReservations: List<Reservation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false,
    val isLoadingCheckIns: Boolean = false,
    val isLoadingCheckOuts: Boolean = false,
    val isLoadingWeekBookings: Boolean = false,
    val isLoadingPendingReservations: Boolean = false,
    val useDebugData: Boolean = false  // New field for debug mode
) : ViewState