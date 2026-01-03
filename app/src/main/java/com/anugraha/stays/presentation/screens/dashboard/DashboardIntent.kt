package com.anugraha.stays.presentation.screens.dashboard

sealed class DashboardIntent {
    object LoadData : DashboardIntent()
    object RefreshData : DashboardIntent()
    data class AcceptReservation(val reservationId: Int) : DashboardIntent()
    data class DeclineReservation(val reservationId: Int) : DashboardIntent()
}