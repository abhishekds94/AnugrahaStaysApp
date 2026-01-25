package com.anugraha.stays.presentation.screens.dashboard

import com.anugraha.stays.util.ViewEffect

sealed class DashboardEffect : ViewEffect {
    data class ShowToast(val message: String) : DashboardEffect()
    data class ShowError(val message: String) : DashboardEffect()
}
