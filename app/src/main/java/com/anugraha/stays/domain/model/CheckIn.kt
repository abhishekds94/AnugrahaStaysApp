package com.anugraha.stays.domain.model

import java.time.LocalDate
import java.time.LocalTime

data class CheckIn(
    val reservation: Reservation,
    val checkInTime: LocalTime?
)

data class CheckOut(
    val reservation: Reservation,
    val checkOutTime: LocalTime?
)