package com.anugraha.stays.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object DateUtils {

    val IST_ZONE: ZoneId = ZoneId.of("Asia/Kolkata")

    fun now(): LocalDate = LocalDate.now(IST_ZONE)
    fun nowDateTime(): LocalDateTime = LocalDateTime.now(IST_ZONE)
    fun nowZoned(): ZonedDateTime = ZonedDateTime.now(IST_ZONE)

    fun parseDate(dateString: String): LocalDate {
        return try {
            if (dateString.contains('T') || dateString.contains(' ')) {
                LocalDateTime.parse(dateString.take(19))
                    .atZone(IST_ZONE)
                    .toLocalDate()
            } else {
                LocalDate.parse(dateString)
            }
        } catch (e: Exception) {
            LocalDate.now(IST_ZONE)
        }
    }

    fun LocalDate.toDisplayFormat(): String {
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
        return this.format(formatter)
    }

    fun LocalDate.toApiFormat(): String {
        return this.toString()
    }

    fun getCurrentWeekDates(): Pair<LocalDate, LocalDate> {
        val today = now()
        val dayOfWeek = today.dayOfWeek.value
        val weekStart = today.minusDays((dayOfWeek - 1).toLong())
        val weekEnd = weekStart.plusDays(6)
        return Pair(weekStart, weekEnd)
    }

    fun LocalDate.isToday(): Boolean {
        return this.isEqual(now())
    }

    fun daysBetween(start: LocalDate, end: LocalDate): Long {
        return ChronoUnit.DAYS.between(start, end)
    }

    fun parseTime(timeString: String): LocalTime? {
        return try {
            LocalTime.parse(timeString)
        } catch (e: Exception) {
            null
        }
    }

    fun getRelativeDayName(date: LocalDate): String {
        val today = now()
        val tomorrow = today.plusDays(1)

        return when {
            date.isEqual(today) -> "Today"
            date.isEqual(tomorrow) -> "Tomorrow"
            else -> {
                val formatter = DateTimeFormatter.ofPattern("EEE")
                date.format(formatter)
            }
        }
    }
}
