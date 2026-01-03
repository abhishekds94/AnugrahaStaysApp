package com.anugraha.stays.domain.model

import java.time.LocalDate

data class Availability(
    val id: Int,
    val date: LocalDate,
    val status: AvailabilityStatus,
    val roomId: Int?,
    val reservation: Reservation? = null,
    val source: String? = null
){
    fun isBlockedByAdmin(): Boolean {
        return status == AvailabilityStatus.CLOSED ||
                (status == AvailabilityStatus.BOOKED && source == "admin")
    }
}