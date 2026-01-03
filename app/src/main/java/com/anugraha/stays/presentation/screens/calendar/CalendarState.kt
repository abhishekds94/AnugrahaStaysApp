package com.anugraha.stays.presentation.screens.calendar

import com.anugraha.stays.domain.model.Availability
import com.anugraha.stays.domain.model.Reservation
import java.time.LocalDate
import java.time.YearMonth

data class CalendarState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate? = null,
    val availabilities: List<Availability> = emptyList(),
    val selectedDateAvailability: Availability? = null,
    val reservations: List<Reservation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,

    // New state for action buttons
    val isActionInProgress: Boolean = false,
    val actionSuccess: Boolean = false,
    val actionError: String? = null
)