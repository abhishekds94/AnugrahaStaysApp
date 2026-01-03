package com.anugraha.stays.util

import java.time.LocalDate

fun String.capitalizeWords(): String {
    return this.split(" ").joinToString(" ") {
        it.lowercase().replaceFirstChar { char -> char.uppercase() }
    }
}

fun Double.toCurrency(): String {
    return "₹${String.format("%.2f", this)}"
}

fun Double.toRupees(): String {
    return "₹${String.format("%.2f", this)}"
}

fun LocalDate.isSameDay(other: LocalDate): Boolean {
    return this.year == other.year &&
            this.monthValue == other.monthValue &&
            this.dayOfMonth == other.dayOfMonth
}