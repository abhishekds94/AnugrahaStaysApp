package com.anugraha.stays.data.remote.dto

import com.anugraha.stays.domain.model.Availability
import com.anugraha.stays.domain.model.AvailabilityStatus
import com.anugraha.stays.util.DateUtils
import com.google.gson.annotations.SerializedName
import java.time.LocalDate

data class AvailabilityDto(
    @SerializedName("id")
    val id: Int? = null,
    @SerializedName("date")
    val date: String? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("room_id")
    val roomId: Int? = null,
    @SerializedName("reservation")
    val reservation: ReservationDto? = null,
    @SerializedName("source")
    val source: String? = null
)

fun AvailabilityDto.toDomain(): Availability? {
    if (id == null || date == null) {
        return null
    }

    return try {
        Availability(
            id = id,
            date = DateUtils.parseDate(date),
            status = AvailabilityStatus.fromString(status ?: "open"),
            roomId = roomId,
            reservation = reservation?.toDomain(),
            source = source
        )
    } catch (e: Exception) {
        android.util.Log.e("AvailabilityDto", "Error converting DTO: ${e.message}")
        null
    }
}