package com.anugraha.stays.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateBookingRequest(
    @SerializedName("guest_name")
    val guestName: String,

    @SerializedName("guest_email")
    val guestEmail: String? = null,

    @SerializedName("contact_number")
    val contactNumber: String,

    @SerializedName("check_in_date")
    val checkInDate: String,

    @SerializedName("check_out_date")
    val checkOutDate: String,

    @SerializedName("arrival_time")
    val arrivalTime: String? = null,

    @SerializedName("guests_count")
    val guestsCount: Int,

    @SerializedName("is_pet")
    val isPet: Boolean = false,

    @SerializedName("room_id")
    val roomId: Int,

    @SerializedName("amount_paid")
    val amountPaid: Double? = null,

    @SerializedName("transaction_id")
    val transactionId: String? = null,

    @SerializedName("booking_source")
    val bookingSource: String = "Direct"
)

data class CreateBookingResponse(
    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: ReservationDto
)