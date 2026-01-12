package com.anugraha.stays.presentation.screens.dashboard

sealed class DashboardIntent {
    object LoadData : DashboardIntent()
    object RefreshData : DashboardIntent()
    object SyncExternalBookings : DashboardIntent()  // ADD THIS
    data class AcceptReservation(val id: Int) : DashboardIntent()
    data class DeclineReservation(val id: Int) : DashboardIntent()
    object ForceResync : DashboardIntent()
}