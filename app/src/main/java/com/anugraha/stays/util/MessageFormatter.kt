package com.anugraha.stays.util

import com.anugraha.stays.domain.model.Reservation
import java.time.format.DateTimeFormatter

/**
 * Utility for formatting messages for WhatsApp and other communication channels
 */
object MessageFormatter {

    /**
     * Create a formatted welcome message for WhatsApp with booking details
     *
     * @param reservation The reservation to create message for
     * @return Formatted welcome message with booking details, guidelines, and contact info
     */
    fun createWelcomeMessage(reservation: Reservation): String {
        val guestName = reservation.primaryGuest.fullName
        val checkInDate = reservation.checkInDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        val checkOutDate = reservation.checkOutDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))

        // Format check-in time
        val checkInTime = reservation.estimatedCheckInTime?.let {
            try {
                it.format(DateTimeFormatter.ofPattern("hh:mm a"))
            } catch (e: Exception) {
                "After 2:00 PM"
            }
        } ?: "After 2:00 PM"

        val totalGuests = reservation.adults + reservation.kids
        val roomType = reservation.room?.title ?: "Standard Room"
        val bookingId = reservation.reservationNumber

        return """
Hello $guestName 👋🏼,

Thank you for booking Anugraha Stays, Shivamogga. We're happy to host you!

Your stay has been confirmed. Here are your booking details:
📅 Check-in: $checkInDate, After 2.00 PM
📅 Check-out: $checkOutDate, Before 12:00 PM
👥 Guests: $totalGuests
🏡 Room/Property: $roomType
🧾 Booking ID: $bookingId

A few quick house rules:
• Check-in: After 2:00 PM | Check-out: Before 12:00 PM
• Please keep noise low after 9:00 PM
• Please share a Government-issued ID for all guests before check-in.
• Smoking is strictly not allowed inside the home. Additional cleaning charges may apply if this rule is violated.
• Kindly wash dishes after use and dispose of trash in the bins provided. Extra cleaning fees may apply if dishes or trash are left unattended.
• Please respect the property and keep the space clean, just as you found it.
• Unmarried couples are not permitted at the property.
• Only registered guests are allowed inside the property.
• Please inform us in advance for early check-in or late check-out, and we will try our best based on availability.

📍 Location: https://maps.app.goo.gl/vyxJzqcouhnX9xzS7

If you need help with directions or local places to visit around Shivamogga, feel free to message us anytime.

Looking forward to welcoming you to Anugraha Stays 🌿

Warm regards,
Sheshagiri,
Anugraha Stays
94486-28559
        """.trimIndent()
    }

    /**
     * Create a payment reminder message
     */
    fun createPaymentReminderMessage(
        reservation: Reservation,
        pendingAmount: Double
    ): String {
        val guestName = reservation.primaryGuest.fullName
        val bookingId = reservation.reservationNumber

        return """
Hello $guestName,

This is a friendly reminder regarding your booking at Anugraha Stays.

🧾 Booking ID: $bookingId
💰 Pending Amount: ₹${String.format("%.2f", pendingAmount)}

Please complete the payment at your earliest convenience to confirm your reservation.

For payment details or any questions, feel free to reach out.

Thank you!

Warm regards,
Sheshagiri,
Anugraha Stays
📞 94486-28559
        """.trimIndent()
    }

    /**
     * Create a check-in reminder message (sent day before arrival)
     */
    fun createCheckInReminderMessage(reservation: Reservation): String {
        val guestName = reservation.primaryGuest.fullName
        val checkInDate = reservation.checkInDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        val checkInTime = reservation.estimatedCheckInTime?.let {
            try {
                it.format(DateTimeFormatter.ofPattern("hh:mm a"))
            } catch (e: Exception) {
                "After 2:00 PM"
            }
        } ?: "After 2:00 PM"

        return """
Hello $guestName,

Your check-in at Anugraha Stays is tomorrow! 🎉

📅 Date: $checkInDate
🕒 Time: $checkInTime
📍 Location: https://maps.app.goo.gl/vyxJzqcouhnX9xzS7

A few reminders:
• Check-in starts at 2:00 PM
• Early check-in may be available (please confirm)
• If you need directions or have any questions, just message us

We're looking forward to welcoming you! 🌿

Warm regards,
Sheshagiri,
Anugraha Stays
📞 94486-28559
        """.trimIndent()
    }

    /**
     * Create a thank you message (sent after checkout)
     */
    fun createThankYouMessage(reservation: Reservation): String {
        val guestName = reservation.primaryGuest.fullName

        return """
Hello $guestName,

Thank you for staying with us at Anugraha Stays, Shivamogga! 🙏

We hope you had a wonderful experience. Your feedback means a lot to us, and we'd love to hear about your stay.

If you have any suggestions or if there's anything we could improve, please feel free to share.

We look forward to hosting you again soon! 🌿

Warm regards,
Sheshagiri,
Anugraha Stays
📞 94486-28559
        """.trimIndent()
    }

    /**
     * Create a booking confirmation message (for direct bookings)
     */
    fun createBookingConfirmationMessage(reservation: Reservation): String {
        val guestName = reservation.primaryGuest.fullName
        val checkInDate = reservation.checkInDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        val checkOutDate = reservation.checkOutDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        val bookingId = reservation.reservationNumber
        val totalAmount = reservation.totalAmount

        return """
Hello $guestName,

Your booking at Anugraha Stays has been confirmed! ✅

🧾 Booking ID: $bookingId
📅 Check-in: $checkInDate
📅 Check-out: $checkOutDate
💰 Total Amount: ₹${String.format("%.2f", totalAmount)}

We'll send you more details and directions closer to your check-in date.

If you have any questions in the meantime, feel free to reach out!

Warm regards,
Sheshagiri,
Anugraha Stays
📞 94486-28559
        """.trimIndent()
    }

    /**
     * Create a cancellation confirmation message
     */
    fun createCancellationMessage(
        reservation: Reservation,
        refundAmount: Double? = null
    ): String {
        val guestName = reservation.primaryGuest.fullName
        val bookingId = reservation.reservationNumber

        val refundInfo = if (refundAmount != null && refundAmount > 0) {
            "\n💰 Refund Amount: ₹${String.format("%.2f", refundAmount)}\n(Will be processed within 5-7 business days)"
        } else {
            ""
        }

        return """
Hello $guestName,

Your booking cancellation has been processed.

🧾 Booking ID: $bookingId$refundInfo

We're sorry we couldn't host you this time. We hope to welcome you at Anugraha Stays in the future!

If you have any questions about the cancellation, feel free to reach out.

Warm regards,
Sheshagiri,
Anugraha Stays
📞 94486-28559
        """.trimIndent()
    }

    /**
     * Create a custom message with booking context
     */
    fun createCustomMessage(
        reservation: Reservation,
        customMessage: String
    ): String {
        val guestName = reservation.primaryGuest.fullName
        val bookingId = reservation.reservationNumber

        return """
Hello $guestName,

$customMessage

🧾 Booking ID: $bookingId

Warm regards,
Sheshagiri,
Anugraha Stays
📞 94486-28559
        """.trimIndent()
    }
}