package com.anugraha.stays.util

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Utility class for calculating booking prices based on Anugraha Stays pricing rules.
 *
 * Pricing Rules:
 * - Base price per night (up to 4 guests): ₹2,250
 * - Non A/C Room: Base price only
 * - A/C Room: Base price + ₹500 per A/C room per night
 * - Extra Guests (>4): +₹300 per guest per night
 * - Pets: +₹500 per pet per night
 */
object BookingCalculator {

    // Pricing constants
    private const val BASE_PRICE_PER_NIGHT = 2250.0
    private const val AC_ROOM_CHARGE_PER_NIGHT = 500.0
    private const val EXTRA_GUEST_CHARGE_PER_NIGHT = 300.0
    private const val PET_CHARGE_PER_NIGHT = 500.0
    private const val INCLUDED_GUESTS = 4

    /**
     * Calculate total booking amount
     *
     * @param checkInDate Check-in date
     * @param checkOutDate Check-out date
     * @param numberOfGuests Total number of guests
     * @param isAcRoom Whether the booking is for A/C room(s)
     * @param numberOfAcRooms Number of A/C rooms (only applicable if isAcRoom is true)
     * @param hasPet Whether the booking includes pets
     * @param numberOfPets Number of pets (only applicable if hasPet is true)
     * @return Total booking amount in rupees
     */
    fun calculateTotalAmount(
        checkInDate: LocalDate,
        checkOutDate: LocalDate,
        numberOfGuests: Int = 1,
        isAcRoom: Boolean = false,
        numberOfAcRooms: Int = 1,
        hasPet: Boolean = false,
        numberOfPets: Int = 1
    ): Double {
        val numberOfNights = ChronoUnit.DAYS.between(checkInDate, checkOutDate).toInt()
        if (numberOfNights <= 0) return 0.0

        // Base room charges
        val baseCharges = BASE_PRICE_PER_NIGHT * numberOfNights

        // A/C room charges
        val acCharges = if (isAcRoom) {
            AC_ROOM_CHARGE_PER_NIGHT * numberOfAcRooms * numberOfNights
        } else {
            0.0
        }

        // Extra guest charges (only if more than 4 guests)
        val extraGuestCharges = if (numberOfGuests > INCLUDED_GUESTS) {
            (numberOfGuests - INCLUDED_GUESTS) * EXTRA_GUEST_CHARGE_PER_NIGHT * numberOfNights
        } else {
            0.0
        }

        // Pet charges
        val petCharges = if (hasPet && numberOfPets > 0) {
            numberOfPets * PET_CHARGE_PER_NIGHT * numberOfNights
        } else {
            0.0
        }

        return baseCharges + acCharges + extraGuestCharges + petCharges
    }

    /**
     * Calculate breakdown of charges
     *
     * @return Map of charge categories to amounts
     */
    fun calculateBreakdown(
        checkInDate: LocalDate,
        checkOutDate: LocalDate,
        numberOfGuests: Int = 1,
        isAcRoom: Boolean = false,
        numberOfAcRooms: Int = 1,
        hasPet: Boolean = false,
        numberOfPets: Int = 1
    ): Map<String, Double> {
        val numberOfNights = ChronoUnit.DAYS.between(checkInDate, checkOutDate).toInt()
        if (numberOfNights <= 0) return emptyMap()

        val breakdown = mutableMapOf<String, Double>()

        // Base charges
        breakdown["Base Room Charges"] = BASE_PRICE_PER_NIGHT * numberOfNights

        // A/C charges
        if (isAcRoom) {
            breakdown["A/C Room Charges"] = AC_ROOM_CHARGE_PER_NIGHT * numberOfAcRooms * numberOfNights
        }

        // Extra guest charges
        if (numberOfGuests > INCLUDED_GUESTS) {
            val extraGuests = numberOfGuests - INCLUDED_GUESTS
            breakdown["Extra Guest Charges ($extraGuests guests)"] =
                extraGuests * EXTRA_GUEST_CHARGE_PER_NIGHT * numberOfNights
        }

        // Pet charges
        if (hasPet && numberOfPets > 0) {
            breakdown["Pet Charges ($numberOfPets pet${if (numberOfPets > 1) "s" else ""})"] =
                numberOfPets * PET_CHARGE_PER_NIGHT * numberOfNights
        }

        return breakdown
    }

    /**
     * Get pricing constants for display purposes
     */
    fun getPricingRules(): Map<String, Any> = mapOf(
        "basePrice" to BASE_PRICE_PER_NIGHT,
        "acRoomCharge" to AC_ROOM_CHARGE_PER_NIGHT,
        "extraGuestCharge" to EXTRA_GUEST_CHARGE_PER_NIGHT,
        "petCharge" to PET_CHARGE_PER_NIGHT,
        "includedGuests" to INCLUDED_GUESTS
    )
}