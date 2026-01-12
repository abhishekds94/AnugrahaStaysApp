package com.anugraha.stays.domain.model

enum class ICalSource {
    AIRBNB,
    BOOKING_COM;

    fun toBookingSource(): BookingSource {
        return when (this) {
            AIRBNB -> BookingSource.AIRBNB
            BOOKING_COM -> BookingSource.BOOKING_COM
        }
    }

    fun getDisplayName(): String {
        return when (this) {
            AIRBNB -> "Booking from Airbnb"
            BOOKING_COM -> "Booking from Booking.com"
        }
    }
}

data class ICalConfig(
    val source: ICalSource,
    val url: String
) {
    companion object {
        fun getDefaultConfigs(): List<ICalConfig> {
            return listOf(
                ICalConfig(
                    source = ICalSource.AIRBNB,
                    url = "https://www.airbnb.com/calendar/ical/1094965185005121322.ics?s=b50462d529b36a1c5d2db5810af97020&locale=en"
                ),
                ICalConfig(
                    source = ICalSource.BOOKING_COM,
                    url = "https://ical.booking.com/v1/export?t=8c1ef0e6-e974-4345-827e-6bc8cf58ed6f"
                )
            )
        }
    }
}