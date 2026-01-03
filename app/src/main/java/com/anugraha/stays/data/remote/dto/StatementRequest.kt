package com.anugraha.stays.data.remote.dto

import com.google.gson.annotations.SerializedName

data class StatementRequest(
    @SerializedName("start_date")
    val startDate: String,

    @SerializedName("end_date")
    val endDate: String
)

data class StatementResponse(
    @SerializedName("success")
    val success: Boolean = true,

    @SerializedName("data")
    val data: StatementData? = null
)

data class StatementData(
    @SerializedName("start_date")
    val startDate: String? = null,

    @SerializedName("end_date")
    val endDate: String? = null,

    @SerializedName("total_reservations")
    val totalReservations: Int = 0,

    @SerializedName("total_amount")
    val totalAmount: Double = 0.0,

    @SerializedName("reservations")
    val reservations: List<ReservationDto>? = null
)