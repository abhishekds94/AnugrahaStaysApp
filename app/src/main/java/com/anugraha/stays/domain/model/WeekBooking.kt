package com.anugraha.stays.domain.model

import java.time.LocalDate
import java.time.DayOfWeek

data class WeekBooking(
    val reservation: Reservation,
    val dayOfWeek: DayOfWeek,
    val date: LocalDate
)