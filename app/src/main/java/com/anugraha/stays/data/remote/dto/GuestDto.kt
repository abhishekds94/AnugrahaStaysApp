package com.anugraha.stays.data.remote.dto

import android.util.Log
import com.anugraha.stays.domain.model.Guest
import com.google.gson.annotations.SerializedName

data class GuestDto(
    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("full_name")
    val fullName: String? = null,

    @SerializedName("phone")
    val phone: String? = null,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("address")
    val address: String? = null,

    @SerializedName("arrival_time")
    val arrivalTime: String? = null,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("updated_at")
    val updatedAt: String? = null
)

fun GuestDto.toDomain(): Guest {
    val guestName = fullName ?: "Unknown Guest"
    val guestPhone = phone ?: "No Phone"

    Log.d("GuestDto", "Mapping guest: $guestName, $guestPhone")

    return Guest(
        fullName = guestName,
        phone = guestPhone,
        email = email
    )
}