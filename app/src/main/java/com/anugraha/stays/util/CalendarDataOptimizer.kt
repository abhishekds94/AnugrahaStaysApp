package com.anugraha.stays.util

import com.anugraha.stays.domain.model.Availability
import com.anugraha.stays.domain.model.BookingSource
import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.domain.model.ReservationStatus
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optimized calendar data processor
 * Pre-processes all booking data once instead of scanning on every date
 */
@Singleton
class CalendarDataOptimizer @Inject constructor() {

    /**
     * Pre-processed calendar data for fast lookups
     */
    data class ProcessedCalendarData(
        val directBookingDates: Set<LocalDate>,
        val airbnbDates: Set<LocalDate>,
        val bookingComDates: Set<LocalDate>,
        val blockedDates: Set<LocalDate>
    )

    /**
     * Pre-process all reservations and availabilities for a month
     * Call this ONCE when month data loads, then use for all date lookups
     */
    fun processMonthData(
        yearMonth: YearMonth,
        reservations: List<Reservation>,
        availabilities: List<Availability>
    ): ProcessedCalendarData {
        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()

        val directDates = mutableSetOf<LocalDate>()
        val airbnbDates = mutableSetOf<LocalDate>()
        val bookingComDates = mutableSetOf<LocalDate>()
        val blockedDates = mutableSetOf<LocalDate>()

        // Pre-process blocked dates (fast - just a map lookup)
        blockedDates.addAll(
            availabilities
                .filter { it.isBlockedByAdmin() && it.date in startDate..endDate }
                .map { it.date }
        )

        // Pre-process all reservations once
        reservations.forEach { res ->
            // Skip invalid statuses
            if (res.status !in listOf(ReservationStatus.APPROVED, ReservationStatus.CHECKOUT, ReservationStatus.COMPLETED)) {
                return@forEach
            }

            // Get all dates for this reservation
            val reservationDates = generateDateRange(res.checkInDate, res.checkOutDate, startDate, endDate)

            // Add to appropriate set based on booking source
            when {
                res.bookingSource in listOf(BookingSource.DIRECT, BookingSource.WEBSITE) -> {
                    directDates.addAll(reservationDates)
                }
                res.bookingSource == BookingSource.AIRBNB -> {
                    airbnbDates.addAll(reservationDates)
                }
                res.bookingSource == BookingSource.BOOKING_COM -> {
                    bookingComDates.addAll(reservationDates)
                }
            }
        }

        return ProcessedCalendarData(
            directBookingDates = directDates,
            airbnbDates = airbnbDates,
            bookingComDates = bookingComDates,
            blockedDates = blockedDates
        )
    }

    /**
     * Fast lookup - O(1) instead of O(n)
     */
    fun getBookingType(date: LocalDate, data: ProcessedCalendarData): BookingType {
        return when {
            data.blockedDates.contains(date) -> BookingType.BLOCKED
            data.directBookingDates.contains(date) -> BookingType.DIRECT_OR_WEBSITE
            data.airbnbDates.contains(date) -> BookingType.AIRBNB
            data.bookingComDates.contains(date) -> BookingType.BOOKING_COM
            else -> BookingType.NONE
        }
    }

    /**
     * Generate all dates in a reservation that fall within the month
     */
    private fun generateDateRange(
        checkIn: LocalDate,
        checkOut: LocalDate,
        monthStart: LocalDate,
        monthEnd: LocalDate
    ): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var current = maxOf(checkIn, monthStart)
        val end = minOf(checkOut.minusDays(1), monthEnd) // Exclude checkout date

        while (!current.isAfter(end)) {
            dates.add(current)
            current = current.plusDays(1)
        }

        return dates
    }
}

enum class BookingType {
    NONE,
    BLOCKED,
    DIRECT_OR_WEBSITE,
    AIRBNB,
    BOOKING_COM
}