package com.anugraha.stays.domain.model

enum class ReservationStatus {
    PENDING,
    APPROVED,
    CANCELLED,
    ADMIN_CANCELLED,
    COMPLETED,
    CHECKOUT,
    BLOCKED;

    companion object {
        fun fromString(status: String): ReservationStatus {
            return when (status.lowercase()) {
                "pending" -> PENDING
                "approved" -> APPROVED
                "cancelled" -> CANCELLED
                "admin_cancelled" -> ADMIN_CANCELLED
                "completed" -> COMPLETED
                "checkout" -> CHECKOUT
                "blocked" -> BLOCKED
                else -> PENDING
            }
        }
    }
}