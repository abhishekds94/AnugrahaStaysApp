package com.anugraha.stays.data.remote.dto

import com.anugraha.stays.domain.model.Room
import com.google.gson.annotations.SerializedName

data class RoomDto(
    @SerializedName("id")
    val id: Int? = null,
    @SerializedName("title")
    val title: String? = null
)

fun RoomDto.toDomain() = Room(
    id = id ?: 0,
    title = title ?: "Standard Room"
)