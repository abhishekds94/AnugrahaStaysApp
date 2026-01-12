package com.anugraha.stays.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object DateUtils {

    // ✅ DEFINE IST TIMEZONE
    val IST_ZONE: ZoneId = ZoneId.of("Asia/Kolkata")

    // ✅ Get current date/time in IST
    fun now(): LocalDate = LocalDate.now(IST_ZONE)
    fun nowDateTime(): LocalDateTime = LocalDateTime.now(IST_ZONE)
    fun nowZoned(): ZonedDateTime = ZonedDateTime.now(IST_ZONE)

    /**
     * Parse date string ALWAYS in IST timezone
     */
    fun parseDate(dateString: String): LocalDate {
        return try {
            // If it has time component, parse and extract date in IST
            if (dateString.contains('T') || dateString.contains(' ')) {
                LocalDateTime.parse(dateString.take(19))
                    .atZone(IST_ZONE)
                    .toLocalDate()
            } else {
                LocalDate.parse(dateString)
            }
        } catch (e: Exception) {
            android.util.Log.e("DateUtils", "Error parsing date: $dateString", e)
            LocalDate.now(IST_ZONE)
        }
    }

    /**
     * Format date for display
     */
    fun LocalDate.toDisplayFormat(): String {
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
        return this.format(formatter)
    }

    /**
     * Format date for API (YYYY-MM-DD)
     */
    fun LocalDate.toApiFormat(): String {
        return this.toString() // ISO format
    }

    /**
     * Get current week dates (Monday to Sunday) in IST
     */
    fun getCurrentWeekDates(): Pair<LocalDate, LocalDate> {
        val today = now()
        val dayOfWeek = today.dayOfWeek.value
        val weekStart = today.minusDays((dayOfWeek - 1).toLong())
        val weekEnd = weekStart.plusDays(6)
        return Pair(weekStart, weekEnd)
    }

    /**
     * Check if date is today in IST
     */
    fun LocalDate.isToday(): Boolean {
        return this.isEqual(now())
    }

    /**
     * Get days between dates
     */
    fun daysBetween(start: LocalDate, end: LocalDate): Long {
        return ChronoUnit.DAYS.between(start, end)
    }

    /**
     * Parse time string
     */
    fun parseTime(timeString: String): LocalTime? {
        return try {
            LocalTime.parse(timeString)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get relative day name (Today, Tomorrow, or day of week)
     * ✅ ADDED THIS FUNCTION
     */
    fun getRelativeDayName(date: LocalDate): String {
        val today = now()
        val tomorrow = today.plusDays(1)

        return when {
            date.isEqual(today) -> "Today"
            date.isEqual(tomorrow) -> "Tomorrow"
            else -> {
                // Return day of week (Mon, Tue, etc.)
                val formatter = DateTimeFormatter.ofPattern("EEE")
                date.format(formatter)
            }
        }
    }

    /**
     * Debug function to verify IST is working
     */
    fun logCurrentTime() {
        val istNow = now()
        val deviceNow = LocalDate.now() // Device timezone

        android.util.Log.d("DateUtils", "========== TIMEZONE CHECK ==========")
        android.util.Log.d("DateUtils", "IST Date: $istNow")
        android.util.Log.d("DateUtils", "Device Date: $deviceNow")
        android.util.Log.d("DateUtils", "Match: ${istNow == deviceNow}")
        android.util.Log.d("DateUtils", "IST Zone: $IST_ZONE")
        android.util.Log.d("DateUtils", "Device Zone: ${ZoneId.systemDefault()}")
        android.util.Log.d("DateUtils", "====================================")
    }
}