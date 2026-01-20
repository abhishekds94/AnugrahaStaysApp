package com.anugraha.stays.domain.model

data class Room(
    val id: Int,
    val title: String,
    val description: String? = null,
    val data: RoomData? = null
)

data class RoomData(
    val airConditioned: Boolean = false
)