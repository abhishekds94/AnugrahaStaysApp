package com.anugraha.stays.presentation.screens.calendar

import com.anugraha.stays.util.ViewEffect

sealed class CalendarEffect : ViewEffect {
    data class ShowToast(val message: String) : CalendarEffect()
    data class ShowError(val message: String) : CalendarEffect()
}
