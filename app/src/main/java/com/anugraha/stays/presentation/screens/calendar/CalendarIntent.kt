package com.anugraha.stays.presentation.screens.calendar

import com.anugraha.stays.util.ViewIntent
import java.time.LocalDate
import java.time.YearMonth

sealed class CalendarIntent : ViewIntent {
    data class LoadMonth(val yearMonth: YearMonth) : CalendarIntent()
    data class DateSelected(val date: LocalDate) : CalendarIntent()
    data class ToggleAvailability(val date: LocalDate) : CalendarIntent()
    object LoadBookings : CalendarIntent()
    object PreviousMonth : CalendarIntent()
    object NextMonth : CalendarIntent()
    data class BlockDate(val date: LocalDate) : CalendarIntent()
    data class OpenDate(val date: LocalDate) : CalendarIntent()
    data class CancelBooking(val reservationId: Int, val date: LocalDate) : CalendarIntent()
}
