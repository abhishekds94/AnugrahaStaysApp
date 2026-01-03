package com.anugraha.stays.presentation.screens.calendar

import java.time.LocalDate
import java.time.YearMonth

sealed class CalendarIntent {
    data class LoadMonth(val yearMonth: YearMonth) : CalendarIntent()
    data class DateSelected(val date: LocalDate) : CalendarIntent()
    data class ToggleAvailability(val date: LocalDate) : CalendarIntent()
    object LoadBookings : CalendarIntent()
    object PreviousMonth : CalendarIntent()
    object NextMonth : CalendarIntent()

    // New intents for blocking/opening dates and canceling bookings
    data class BlockDate(val date: LocalDate) : CalendarIntent()
    data class OpenDate(val date: LocalDate) : CalendarIntent()
    data class CancelBooking(val reservationId: Int, val date: LocalDate) : CalendarIntent()
}