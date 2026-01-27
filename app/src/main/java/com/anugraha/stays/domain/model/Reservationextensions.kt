package com.anugraha.stays.domain.model

import com.anugraha.stays.util.BookingCalculator

/**
 * Calculate the expected amount based on booking details
 * Returns null if calculation fails (safer approach)
 */
fun Reservation.calculateExpectedAmount(): Double? {
    return try {
        // Determine if room is AC
        val isAcRoom = room?.data?.airConditioned ?: false

        // For AC rooms, we need to know how many AC rooms
        // Since we don't have this data stored, we'll assume 1 AC room if room is AC
        val numberOfAcRooms = if (isAcRoom) 1 else 0

        BookingCalculator.calculateTotalAmount(
            checkInDate = checkInDate,
            checkOutDate = checkOutDate,
            numberOfGuests = adults + kids,
            isAcRoom = isAcRoom,
            numberOfAcRooms = numberOfAcRooms,
            hasPet = hasPet,
            numberOfPets = if (hasPet) 1 else 0 // Assume 1 pet if hasPet is true
        )
    } catch (e: Exception) {
        // If calculation fails, return null instead of crashing
        null
    }
}

/**
 * Get the pending balance (difference between calculated and paid amount)
 * Returns 0.0 if calculation fails (safe default)
 */
fun Reservation.getPendingBalance(): Double {
    return try {
        val calculatedAmount = calculateExpectedAmount() ?: return 0.0
        calculatedAmount - totalAmount
    } catch (e: Exception) {
        0.0
    }
}

/**
 * Check if there's a pending balance (amount mismatch)
 * Returns false if calculation fails (safe default)
 */
fun Reservation.hasPendingBalance(): Boolean {
    return try {
        val balance = getPendingBalance()
        balance != 0.0 && calculateExpectedAmount() != null
    } catch (e: Exception) {
        false
    }
}

/**
 * Check if the booking is underpaid
 */
fun Reservation.isUnderpaid(): Boolean {
    return try {
        getPendingBalance() > 0.0
    } catch (e: Exception) {
        false
    }
}

/**
 * Check if the booking is overpaid
 */
fun Reservation.isOverpaid(): Boolean {
    return try {
        getPendingBalance() < 0.0
    } catch (e: Exception) {
        false
    }
}

/**
 * Get formatted balance text for display
 */
fun Reservation.getBalanceText(): String {
    return try {
        val balance = getPendingBalance()
        when {
            balance > 0 -> "Pending: ₹${String.format("%.2f", balance)}"
            balance < 0 -> "Excess: ₹${String.format("%.2f", -balance)}"
            else -> "Settled"
        }
    } catch (e: Exception) {
        "Settled"
    }
}

/**
 * Get balance message for notifications/popups
 */
fun Reservation.getBalanceMessage(): String {
    return try {
        val balance = getPendingBalance()
        val guestName = primaryGuest.fullName

        when {
            balance > 0 -> "The $guestName has a pending balance of Rs. ${String.format("%.0f", balance)}"
            balance < 0 -> "The $guestName has an excess payment of Rs. ${String.format("%.0f", -balance)}"
            else -> "Payment is settled for $guestName"
        }
    } catch (e: Exception) {
        "Payment is settled for ${primaryGuest.fullName}"
    }
}