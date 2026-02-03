package com.anugraha.stays.util

import android.util.Log
import com.anugraha.stays.domain.model.BookingSource
import com.anugraha.stays.domain.model.Reservation
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Advanced booking deduplicator that handles:
 * 1. Cross-platform duplicates (Airbnb blocking Booking.com dates)
 * 2. Direct booking conflicts (Direct booking blocked on external platforms)
 */
@Singleton
class AdvancedBookingDeduplicator @Inject constructor() {

    /**
     * Remove all types of duplicates:
     * - Airbnb vs Booking.com duplicates
     * - Direct bookings appearing as external blocks
     */
    fun deduplicateAllBookings(reservations: List<Reservation>): List<Reservation> {
        Log.d("AdvancedDedup", "=== Starting Advanced Deduplication ===")
        Log.d("AdvancedDedup", "Total input reservations: ${reservations.size}")

        val directBookings = reservations.filter {
            it.bookingSource == BookingSource.DIRECT || it.bookingSource == BookingSource.WEBSITE
        }

        val externalBookings = reservations.filter {
            it.bookingSource == BookingSource.AIRBNB || it.bookingSource == BookingSource.BOOKING_COM
        }

        val otherBookings = reservations.filter {
            it.bookingSource !in listOf(
                BookingSource.DIRECT,
                BookingSource.WEBSITE,
                BookingSource.AIRBNB,
                BookingSource.BOOKING_COM
            )
        }

        Log.d("AdvancedDedup", "Direct bookings: ${directBookings.size}")
        Log.d("AdvancedDedup", "External bookings: ${externalBookings.size}")
        Log.d("AdvancedDedup", "Other bookings: ${otherBookings.size}")

        // Step 1: Remove external bookings that conflict with direct bookings
        val externalWithoutDirectConflicts = removeDirectConflicts(externalBookings, directBookings)

        // Step 2: Deduplicate remaining external bookings (Airbnb vs Booking.com)
        val deduplicatedExternal = deduplicateExternalBookings(externalWithoutDirectConflicts)

        val finalList = directBookings + deduplicatedExternal + otherBookings

        Log.d("AdvancedDedup", "=== Deduplication Complete ===")
        Log.d("AdvancedDedup", "Input: ${reservations.size} bookings")
        Log.d("AdvancedDedup", "Output: ${finalList.size} bookings")
        Log.d("AdvancedDedup", "Removed: ${reservations.size - finalList.size} duplicates")

        return finalList
    }

    /**
     * Remove external bookings that overlap with direct bookings
     *
     * When you make a direct booking, your calendar blocks those dates.
     * Airbnb/Booking.com then see "Not available" and create a blocked entry.
     * We need to remove these external blocks since the direct booking is the truth.
     */
    private fun removeDirectConflicts(
        externalBookings: List<Reservation>,
        directBookings: List<Reservation>
    ): List<Reservation> {
        if (directBookings.isEmpty()) {
            Log.d("AdvancedDedup", "No direct bookings, skipping conflict check")
            return externalBookings
        }

        val withoutConflicts = externalBookings.filterNot { external ->
            val hasConflict = directBookings.any { direct ->
                datesOverlap(external, direct)
            }

            if (hasConflict) {
                Log.d("AdvancedDedup", "❌ Removing external conflict: ${external.bookingSource.name} ${external.checkInDate} to ${external.checkOutDate}")
                Log.d("AdvancedDedup", "   Conflicts with direct booking on same dates")
            }

            hasConflict
        }

        val removed = externalBookings.size - withoutConflicts.size
        if (removed > 0) {
            Log.d("AdvancedDedup", "✅ Removed $removed external bookings that conflict with direct bookings")
        }

        return withoutConflicts
    }

    /**
     * Deduplicate between Airbnb and Booking.com
     */
    private fun deduplicateExternalBookings(externalBookings: List<Reservation>): List<Reservation> {
        if (externalBookings.size < 2) {
            return externalBookings
        }

        Log.d("AdvancedDedup", "--- Deduplicating External Platforms ---")

        val duplicateGroups = findDuplicateGroups(externalBookings)
        Log.d("AdvancedDedup", "Found ${duplicateGroups.size} external duplicate groups")

        return removeDuplicates(externalBookings, duplicateGroups)
    }

    /**
     * Find groups of bookings with overlapping dates
     */
    private fun findDuplicateGroups(bookings: List<Reservation>): List<List<Reservation>> {
        val groups = mutableListOf<MutableList<Reservation>>()
        val processed = mutableSetOf<Int>()

        for (i in bookings.indices) {
            if (i in processed) continue

            val booking = bookings[i]
            val group = mutableListOf(booking)
            processed.add(i)

            for (j in i + 1 until bookings.size) {
                if (j in processed) continue

                val other = bookings[j]
                if (datesOverlap(booking, other)) {
                    group.add(other)
                    processed.add(j)
                }
            }

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
        // Exact match
        if (booking1.checkInDate == booking2.checkInDate &&
            booking1.checkOutDate == booking2.checkOutDate) {
            return true
        }

        // Partial overlap
        return !(booking1.checkOutDate <= booking2.checkInDate ||
                booking2.checkOutDate <= booking1.checkInDate)
    }

    /**
     * Remove duplicates, keeping the real booking
     */
    private fun removeDuplicates(
        allBookings: List<Reservation>,
        duplicateGroups: List<List<Reservation>>
    ): List<Reservation> {
        val toRemove = mutableSetOf<Int>()

        duplicateGroups.forEach { group ->
            val realBooking = identifyRealBooking(group)

            group.forEach { booking ->
                if (booking.id != realBooking.id) {
                    toRemove.add(booking.id)
                    Log.d("AdvancedDedup", "❌ Removing external duplicate: ${booking.bookingSource.name} ${booking.checkInDate} to ${booking.checkOutDate}")
                    Log.d("AdvancedDedup", "   Kept: ${realBooking.bookingSource.name}")
                }
            }
        }

        return allBookings.filterNot { it.id in toRemove }
    }

    /**
     * Identify which booking is real vs automatic block
     */
    private fun identifyRealBooking(group: List<Reservation>): Reservation {
        val scored = group.map { booking ->
            booking to calculateBookingScore(booking)
        }.sortedByDescending { it.second }

        Log.d("AdvancedDedup", "  Duplicate group scores:")
        scored.forEach { (booking, score) ->
            Log.d("AdvancedDedup", "    ${booking.bookingSource.name}: $score (${booking.checkInDate})")
        }

        return scored.first().first
    }

    /**
     * Calculate score (higher = more likely real booking)
     */
    private fun calculateBookingScore(booking: Reservation): Int {
        var score = 0
        val summary = booking.reservationNumber.lowercase()

        // Negative indicators
        when {
            summary.contains("not available") -> score -= 100
            summary.contains("blocked") -> score -= 100
            summary.contains("unavailable") -> score -= 100
            summary.contains("closed") -> score -= 100
            summary == "busy" || summary == "reserved" -> score -= 50
        }

        // Positive indicators
        when {
            summary.contains("reservation") && !summary.contains("not available") -> score += 50
            summary.contains("booking") && !summary.contains("not available") -> score += 30
            summary.contains("confirmed") -> score += 40
            summary.length > 20 -> score += 20
        }

        // Guest info
        val guestName = booking.primaryGuest.fullName
        if (guestName.isNotBlank() &&
            guestName != "Guest" &&
            guestName != "Unknown" &&
            !guestName.contains("Booking on")) {
            score += 100
        }

        // Source preference
        when (booking.bookingSource) {
            BookingSource.AIRBNB -> score += 5
            BookingSource.BOOKING_COM -> score += 3
            else -> {}
        }

        return score
    }
}