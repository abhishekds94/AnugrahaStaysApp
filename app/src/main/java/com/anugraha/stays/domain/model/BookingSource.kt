package com.anugraha.stays.domain.model

enum class BookingSource {
    DIRECT,
    WEBSITE,
    AIRBNB,
    BOOKING_COM;

    companion object {
        fun fromString(source: String): BookingSource {
            return when (source.lowercase()) {
                "direct" -> DIRECT
                "website" -> WEBSITE
                "airbnb" -> AIRBNB
                "booking.com", "booking_com" -> BOOKING_COM
                else -> DIRECT
            }
        }
    }

    fun displayName(): String {
        return when (this) {
            DIRECT -> "Direct"
            WEBSITE -> "Website"
            AIRBNB -> "Airbnb"
            BOOKING_COM -> "Booking.com"
        }
    }

    // ADD THESE HELPER METHODS
    fun isExternal(): Boolean {
        return this == AIRBNB || this == BOOKING_COM
    }

    fun getExternalDisplayName(): String {
        return when (this) {
            AIRBNB -> "Booking from Airbnb"
            BOOKING_COM -> "Booking from Booking.com"
            else -> displayName()
        }
    }
}