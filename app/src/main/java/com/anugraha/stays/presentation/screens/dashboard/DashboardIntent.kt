package com.anugraha.stays.presentation.screens.dashboard

import com.anugraha.stays.util.ViewIntent

sealed class DashboardIntent : ViewIntent {
    object LoadData : DashboardIntent()
    object RefreshData : DashboardIntent()
    object SyncExternalBookings : DashboardIntent()
    data class AcceptReservation(val id: Int) : DashboardIntent()
    data class DeclineReservation(val id: Int) : DashboardIntent()
    object ForceResync : DashboardIntent()
}
