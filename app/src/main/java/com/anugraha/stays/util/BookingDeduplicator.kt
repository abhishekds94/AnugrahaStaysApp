package com.anugraha.stays.util

import android.util.Log
import com.anugraha.stays.domain.model.BookingSource
import com.anugraha.stays.domain.model.Reservation
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deduplicates external bookings from Airbnb and Booking.com
 *
 * When a guest books on Airbnb, Airbnb automatically blocks those dates on Booking.com
 * (and vice versa), resulting in duplicate bookings for the same dates.
 *
 * This class identifies and removes these duplicates, keeping only the real booking.
 */
@Singleton
class BookingDeduplicator @Inject constructor() {

    /**
     * Remove duplicate bookings from external sources
     *
     * Strategy:
     * 1. Group bookings by overlapping dates
     * 2. Within each group, identify the real booking vs automatic block
     * 3. Keep only the real booking
     */
    fun deduplicateExternalBookings(reservations: List<Reservation>): List<Reservation> {
        val externalBookings = reservations.filter {
            it.bookingSource == BookingSource.AIRBNB || it.bookingSource == BookingSource.BOOKING_COM
        }

        val otherBookings = reservations.filter {
            it.bookingSource != BookingSource.AIRBNB && it.bookingSource != BookingSource.BOOKING_COM
        }

        Log.d("BookingDeduplicator", "=== Starting Deduplication ===")
        Log.d("BookingDeduplicator", "Total external bookings: ${externalBookings.size}")
        externalBookings.forEach { booking ->
            Log.d("BookingDeduplicator", "  ${booking.bookingSource.name}: ${booking.checkInDate} to ${booking.checkOutDate}")
            Log.d("BookingDeduplicator", "    Summary/ReservationNumber: ${booking.reservationNumber}")
        }

        if (externalBookings.size < 2) {
            Log.d("BookingDeduplicator", "No duplicates possible (less than 2 bookings)")
            return reservations
        }

        // Find duplicates
        val duplicateGroups = findDuplicateGroups(externalBookings)
        Log.d("BookingDeduplicator", "Found ${duplicateGroups.size} duplicate groups")

        // Remove duplicates
        val deduplicatedExternalBookings = removeDuplicates(externalBookings, duplicateGroups)

        val removedCount = externalBookings.size - deduplicatedExternalBookings.size
        if (removedCount > 0) {
            Log.d("BookingDeduplicator", "✅ Removed $removedCount duplicate bookings")
        } else {
            Log.d("BookingDeduplicator", "⚠️ No duplicates removed")
        }

        return deduplicatedExternalBookings + otherBookings
    }

    /**
     * Find groups of bookings that overlap (potential duplicates)
     */
    private fun findDuplicateGroups(bookings: List<Reservation>): List<List<Reservation>> {
        val groups = mutableListOf<MutableList<Reservation>>()
        val processed = mutableSetOf<Int>()

        for (i in bookings.indices) {
            if (i in processed) continue

            val booking = bookings[i]
            val group = mutableListOf(booking)
            processed.add(i)

            // Find all bookings that overlap with this one
            for (j in i + 1 until bookings.size) {
                if (j in processed) continue

                val other = bookings[j]
                if (datesOverlap(booking, other)) {
                    group.add(other)
                    processed.add(j)
                }
            }

            // Only add if it's actually a duplicate group (2+ bookings)
            if (group.size > 1) {
                groups.add(group)
            }
        }

        return groups
    }

    /**
     * Check if two bookings have overlapping dates
     */
    private fun datesOverlap(booking1: Reservation, booking2: Reservation): Boolean {
        // Exact match (same check-in and check-out)
        if (booking1.checkInDate == booking2.checkInDate &&
            booking1.checkOutDate == booking2.checkOutDate) {
            return true
        }

        // Partial overlap
        return !(booking1.checkOutDate <= booking2.checkInDate ||
                booking2.checkOutDate <= booking1.checkInDate)
    }

    /**
     * Remove duplicates from groups, keeping only the real booking
     */
    private fun removeDuplicates(
        allBookings: List<Reservation>,
        duplicateGroups: List<List<Reservation>>
    ): List<Reservation> {
        val toRemove = mutableSetOf<Int>()

        duplicateGroups.forEach { group ->
            val realBooking = identifyRealBooking(group)

            // Mark all others in the group for removal
            group.forEach { booking ->
                if (booking.id != realBooking.id) {
                    toRemove.add(booking.id)
                    Log.d("BookingDeduplicator",
                        "Removing duplicate: ${booking.bookingSource.name} " +
                                "${booking.checkInDate} to ${booking.checkOutDate} " +
                                "(Kept: ${realBooking.bookingSource.name})")
                }
            }
        }

        return allBookings.filterNot { it.id in toRemove }
    }

    /**
     * Identify which booking is the real one vs automatic block
     *
     * Rules (in priority order):
     * 1. Booking with guest information (real booking has guest details)
     * 2. Booking with more detailed summary (real booking has more info)
     * 3. Booking from source with "Not available" or "Blocked" = automatic block
     * 4. If still ambiguous, keep Airbnb (as it's typically the primary platform)
     */
    private fun identifyRealBooking(group: List<Reservation>): Reservation {
        // Rule 1: Check for guest information
        val withGuestInfo = group.filter {
            it.primaryGuest.fullName.isNotBlank() &&
                    it.primaryGuest.fullName != "Guest" &&
                    it.primaryGuest.fullName != "Unknown"
        }
        if (withGuestInfo.size == 1) {
            return withGuestInfo.first()
        }

        // Rule 2 & 3: Analyze summary text
        val scored = group.map { booking ->
            booking to calculateBookingScore(booking)
        }.sortedByDescending { it.second }

        Log.d("BookingDeduplicator", "Duplicate group scores:")
        scored.forEach { (booking, score) ->
            Log.d("BookingDeduplicator",
                "  ${booking.bookingSource.name}: $score " +
                        "(${booking.checkInDate} to ${booking.checkOutDate})")
        }

        return scored.first().first
    }

    /**
     * Calculate score for a booking (higher = more likely to be real)
     */
    private fun calculateBookingScore(booking: Reservation): Int {
        var score = 0

        val summary = booking.reservationNumber.lowercase()

        Log.d("BookingDeduplicator", "    Analyzing: ${booking.bookingSource.name} - '$summary'")

        // Negative indicators (automatic blocks)
        when {
            summary.contains("not available") -> {
                score -= 100
                Log.d("BookingDeduplicator", "      -100 (not available)")
            }
            summary.contains("blocked") -> {
                score -= 100
                Log.d("BookingDeduplicator", "      -100 (blocked)")
            }
            summary.contains("unavailable") -> {
                score -= 100
                Log.d("BookingDeduplicator", "      -100 (unavailable)")
            }
            summary.contains("airbnb (not available)") -> {
                score -= 100
                Log.d("BookingDeduplicator", "      -100 (airbnb not available)")
            }
            summary.contains("booking.com (not available)") -> {
                score -= 100
                Log.d("BookingDeduplicator", "      -100 (booking.com not available)")
            }
            summary == "busy" || summary == "reserved" -> {
                score -= 50
                Log.d("BookingDeduplicator", "      -50 (generic block)")
            }
        }

        // Positive indicators (real bookings)
        when {
            summary.contains("reservation") -> {
                score += 50
                Log.d("BookingDeduplicator", "      +50 (reservation)")
            }
            summary.contains("booking") && !summary.contains("not available") -> {
                score += 30
                Log.d("BookingDeduplicator", "      +30 (booking)")
            }
            summary.contains("confirmed") -> {
                score += 40
                Log.d("BookingDeduplicator", "      +40 (confirmed)")
            }
            summary.length > 20 -> {
                score += 20
                Log.d("BookingDeduplicator", "      +20 (long summary)")
            }
        }

        // Guest information
        val guestName = booking.primaryGuest.fullName
        if (guestName.isNotBlank() &&
            guestName != "Guest" &&
            guestName != "Unknown" &&
            !guestName.contains("Booking on")) {
            score += 100
            Log.d("BookingDeduplicator", "      +100 (has guest info)")
        }

        // Source preference (slight advantage to Airbnb if everything else is equal)
        when (booking.bookingSource) {
            BookingSource.AIRBNB -> {
                score += 5
                Log.d("BookingDeduplicator", "      +5 (Airbnb preference)")
            }
            BookingSource.BOOKING_COM -> {
                score += 3
                Log.d("BookingDeduplicator", "      +3 (Booking.com preference)")
            }
            else -> {}
        }

        Log.d("BookingDeduplicator", "      TOTAL SCORE: $score")
        return score
    }

    /**
     * Alternative: Deduplicate by exact date match only (simpler but less accurate)
     */
    fun deduplicateByExactDates(reservations: List<Reservation>): List<Reservation> {
        val externalBookings = reservations.filter {
            it.bookingSource == BookingSource.AIRBNB || it.bookingSource == BookingSource.BOOKING_COM
        }

        val otherBookings = reservations.filter {
            it.bookingSource != BookingSource.AIRBNB && it.bookingSource != BookingSource.BOOKING_COM
        }

        // Group by exact dates
        val grouped = externalBookings.groupBy {
            "${it.checkInDate}_${it.checkOutDate}"
        }

        // For each group with duplicates, keep only one
        val deduplicated = grouped.flatMap { (_, bookings) ->
            if (bookings.size > 1) {
                Log.d("BookingDeduplicator",
                    "Found exact date duplicate: ${bookings[0].checkInDate} to ${bookings[0].checkOutDate}")
                listOf(identifyRealBooking(bookings))
            } else {
                bookings
            }
        }

        return deduplicated + otherBookings
    }
}