package com.anugraha.stays.util

import androidx.core.net.toUri

/**
 * Utility for formatting phone numbers for WhatsApp
 */
object PhoneNumberUtils {

    /**
     * Format phone number for WhatsApp with country code
     *
     * Examples:
     * - "8151848972" → "+918151848972"
     * - "+918151848972" → "+918151848972" (already formatted)
     * - "918151848972" → "+918151848972"
     * - "+91 8151848972" → "+918151848972" (removes space)
     * - "81 5184 8972" → "+918151848972" (removes all spaces)
     */
    fun formatForWhatsApp(phoneNumber: String, countryCode: String = "+91"): String {
        // Remove all spaces, dashes, and other formatting
        val cleanNumber = phoneNumber.replace(Regex("[\\s\\-().]"), "")

        // Already has + prefix
        if (cleanNumber.startsWith("+")) {
            return cleanNumber
        }

        // Already has country code without +
        if (cleanNumber.startsWith(countryCode.removePrefix("+"))) {
            return "+$cleanNumber"
        }

        // Add country code
        return "$countryCode$cleanNumber"
    }

    /**
     * Open WhatsApp with pre-filled message
     *
     * @param context Android context
     * @param phoneNumber Phone number (will be auto-formatted)
     * @param message Optional pre-filled message
     */
    fun openWhatsApp(
        context: android.content.Context,
        phoneNumber: String,
        message: String = ""
    ) {
        val formattedNumber = formatForWhatsApp(phoneNumber)

        // Remove + for WhatsApp URL (WhatsApp expects format: 918151848972)
        val whatsappNumber = formattedNumber.removePrefix("+")

        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            data = ("https://wa.me/$whatsappNumber" +
                    if (message.isNotEmpty()) "?text=${android.net.Uri.encode(message)}" else "").toUri()
            `package` = "com.whatsapp"
        }

        try {
            context.startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            // WhatsApp not installed, try WhatsApp Business
            intent.`package` = "com.whatsapp.w4b"
            try {
                context.startActivity(intent)
            } catch (e2: android.content.ActivityNotFoundException) {
                // Neither WhatsApp nor WhatsApp Business installed
                // Open in browser instead
                val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    data = ("https://wa.me/$whatsappNumber" +
                            if (message.isNotEmpty()) "?text=${android.net.Uri.encode(message)}" else "").toUri()
                }
                context.startActivity(browserIntent)
            }
        }
    }

    /**
     * Format phone number for display
     *
     * Examples:
     * - "8151848972" → "+91 81518 48972"
     * - "+918151848972" → "+91 81518 48972"
     */
    fun formatForDisplay(phoneNumber: String, countryCode: String = "+91"): String {
        val formatted = formatForWhatsApp(phoneNumber, countryCode)

        // For Indian numbers: +91 XXXXX XXXXX
        if (formatted.startsWith("+91") && formatted.length == 13) {
            return "${formatted.substring(0, 3)} ${formatted.substring(3, 8)} ${formatted.substring(8)}"
        }

        // For other numbers, just add space after country code
        return formatted.replaceFirst(Regex("^(\\+\\d{1,4})"), "$1 ")
    }
}