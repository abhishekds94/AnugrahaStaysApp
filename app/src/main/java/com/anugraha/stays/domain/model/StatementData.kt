package com.anugraha.stays.domain.model

data class Statement(
    val totalRevenue: Double,
    val totalBookings: Int,
    val reservations: List<Reservation>
)