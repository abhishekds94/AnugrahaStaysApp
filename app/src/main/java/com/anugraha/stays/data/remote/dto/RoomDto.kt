package com.anugraha.stays.data.remote.dto

import com.anugraha.stays.domain.model.Room
import com.anugraha.stays.domain.model.RoomData
import com.google.gson.annotations.SerializedName
import org.json.JSONObject

data class RoomDto(
    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("data")
    val data: String? = null
)

fun RoomDto.toDomain() = Room(
    id = id ?: 0,
    title = title ?: "Standard Room",
    description = description,
    data = parseRoomData(data)
)

private fun parseRoomData(dataJson: String?): RoomData? {
    if (dataJson.isNullOrEmpty()) return null

    return try {
        val jsonObject = JSONObject(dataJson)
        RoomData(
            airConditioned = jsonObject.optBoolean("air_conditioned", false)
        )
    } catch (e: Exception) {
        android.util.Log.e("RoomDto", "Error parsing room data: ${e.message}")
        null
    }
}