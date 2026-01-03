package com.anugraha.stays.util

import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

object DateUtils {
    private val apiFormatter = DateTimeFormatter.ofPattern(Constants.API_DATE_FORMAT)
    private val displayFormatter = DateTimeFormatter.ofPattern(Constants.DISPLAY_DATE_FORMAT)
    private val timeFormatter = DateTimeFormatter.ofPattern(Constants.TIME_FORMAT)

    fun LocalDate.toApiFormat(): String = this.format(apiFormatter)

    fun LocalDate.toDisplayFormat(): String = this.format(displayFormatter)

    fun LocalTime.toDisplayFormat(): String = this.format(timeFormatter)

    fun String.toLocalDate(): LocalDate = LocalDate.parse(this, apiFormatter)

    fun String.toLocalTime(): LocalTime = LocalTime.parse(this, timeFormatter)

    // UPDATED: Get current week dates (Sunday to Saturday)
    fun getCurrentWeekDates(): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
        return Pair(startOfWeek, endOfWeek)
    }

    // NEW: Get week range as list of dates
    fun getCurrentWeekDatesList(): List<LocalDate> {
        val today = LocalDate.now()
        val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        return (0..6).map { startOfWeek.plusDays(it.toLong()) }
    }

    fun isToday(date: LocalDate): Boolean = date == LocalDate.now()

    fun isPast(date: LocalDate): Boolean = date.isBefore(LocalDate.now())

    fun isFuture(date: LocalDate): Boolean = date.isAfter(LocalDate.now())

    fun getDayOfWeekName(date: LocalDate): String {
        return date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
    }

    // NEW: Get short day name (Mon, Tue, etc.)
    fun getShortDayOfWeek(date: LocalDate): String {
        return when (date.dayOfWeek) {
            DayOfWeek.SUNDAY -> "Sun"
            DayOfWeek.MONDAY -> "Mon"
            DayOfWeek.TUESDAY -> "Tue"
            DayOfWeek.WEDNESDAY -> "Wed"
            DayOfWeek.THURSDAY -> "Thu"
            DayOfWeek.FRIDAY -> "Fri"
            DayOfWeek.SATURDAY -> "Sat"
        }
    }

    // NEW: Format as "Tomorrow", "Today", or day name
    fun getRelativeDayName(date: LocalDate): String {
        val today = LocalDate.now()
        return when {
            date.isEqual(today) -> "Today"
            date.isEqual(today.plusDays(1)) -> "Tomorrow"
            date.isEqual(today.minusDays(1)) -> "Yesterday"
            else -> getDayOfWeekName(date)
        }
    }

    fun isWithinRange(date: LocalDate, start: LocalDate, end: LocalDate): Boolean {
        return !date.isBefore(start) && !date.isAfter(end)
    }

    fun daysFromToday(date: LocalDate): Long {
        return Period.between(LocalDate.now(), date).days.toLong()
    }
}