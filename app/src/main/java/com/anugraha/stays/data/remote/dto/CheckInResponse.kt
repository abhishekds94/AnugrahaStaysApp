package com.anugraha.stays.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CheckInResponse(
    @SerializedName("data")
    val data: List<ReservationDto>? = null
)

data class ReservationsResponse(
    @SerializedName("data")
    val data: List<ReservationDto>? = null
)

data class AvailabilityResponse(
    @SerializedName("data")
    val data: List<AvailabilityDto>? = null
)

data class MessageResponse(
    @SerializedName("message")
    val message: String
)