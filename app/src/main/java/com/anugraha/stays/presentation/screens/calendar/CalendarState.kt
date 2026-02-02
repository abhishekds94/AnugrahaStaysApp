package com.anugraha.stays.presentation.screens.calendar

import com.anugraha.stays.domain.model.Availability
import com.anugraha.stays.domain.model.ICalSource
import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.util.CalendarDataOptimizer
import com.anugraha.stays.util.ViewState
import java.time.LocalDate
import java.time.YearMonth

data class CalendarState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate? = null,
    val availabilities: List<Availability> = emptyList(),
    val selectedDateAvailability: Availability? = null,
    val reservations: List<Reservation> = emptyList(),
    val processedCalendarData: CalendarDataOptimizer.ProcessedCalendarData? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isActionInProgress: Boolean = false,
    val actionSuccess: Boolean = false,
    val actionError: String? = null,
    val failedSources: List<ICalSource> = emptyList(),
    val syncFailureMessage: String? = null
) : ViewState