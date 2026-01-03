package com.anugraha.stays.domain.model

fun availabilityStatusToApiString(status: AvailabilityStatus): String {
    return when (status) {
        AvailabilityStatus.OPEN -> "available"
        AvailabilityStatus.AVAILABLE -> "available"
        AvailabilityStatus.BOOKED -> "booked"
        AvailabilityStatus.CLOSED -> "booked"  // Admin block = "booked"
    }
}

enum class AvailabilityStatus {
    OPEN,
    BOOKED,
    CLOSED,
    AVAILABLE;

    companion object {
        fun fromString(status: String): AvailabilityStatus {
            return when (status.lowercase()) {
                "open" -> OPEN
                "available" -> AVAILABLE
                "booked" -> BOOKED
                "closed" -> CLOSED
                else -> OPEN
            }
        }
    }
}