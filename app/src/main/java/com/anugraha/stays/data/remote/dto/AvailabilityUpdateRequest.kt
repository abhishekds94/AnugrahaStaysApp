package com.anugraha.stays.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AvailabilityUpdateRequest(
    @SerializedName("date_range")
    val dateRange: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("room_id")
    val roomId: Int? = null
)