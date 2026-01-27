package com.anugraha.stays.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages preferences related to booking notifications
 * Tracks which reservations have been seen to avoid duplicate notifications
 */
@Singleton
class BookingNotificationPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * Mark a reservation as seen
     */
    fun markReservationAsSeen(reservationId: Int) {
        val seenIds = getSeenReservationIds().toMutableSet()
        seenIds.add(reservationId)
        prefs.edit().putStringSet(KEY_SEEN_RESERVATION_IDS, seenIds.map { it.toString() }.toSet()).apply()
    }

    /**
     * Mark multiple reservations as seen
     */
    fun markReservationsAsSeen(reservationIds: List<Int>) {
        val seenIds = getSeenReservationIds().toMutableSet()
        seenIds.addAll(reservationIds)
        prefs.edit().putStringSet(KEY_SEEN_RESERVATION_IDS, seenIds.map { it.toString() }.toSet()).apply()
    }

    /**
     * Check if a reservation has been seen
     */
    fun isReservationSeen(reservationId: Int): Boolean {
        return getSeenReservationIds().contains(reservationId)
    }

    /**
     * Get all seen reservation IDs
     */
    fun getSeenReservationIds(): Set<Int> {
        return prefs.getStringSet(KEY_SEEN_RESERVATION_IDS, emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet()
            ?: emptySet()
    }

    /**
     * Clear seen reservations (useful for testing or reset)
     */
    fun clearSeenReservations() {
        prefs.edit().remove(KEY_SEEN_RESERVATION_IDS).apply()
    }

    /**
     * Remove old seen reservations (older than 30 days)
     * This prevents the set from growing indefinitely
     */
    fun cleanupOldSeenReservations(currentReservationIds: List<Int>) {
        // Only keep reservation IDs that are in the current list
        val seenIds = getSeenReservationIds()
        val relevantIds = seenIds.intersect(currentReservationIds.toSet())
        prefs.edit().putStringSet(KEY_SEEN_RESERVATION_IDS, relevantIds.map { it.toString() }.toSet()).apply()
    }

    /**
     * Get/Set last check timestamp
     */
    var lastCheckTimestamp: Long
        get() = prefs.getLong(KEY_LAST_CHECK_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_CHECK_TIMESTAMP, value).apply()

    /**
     * Enable/disable notifications
     */
    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()

    companion object {
        private const val PREFS_NAME = "booking_notification_prefs"
        private const val KEY_SEEN_RESERVATION_IDS = "seen_reservation_ids"
        private const val KEY_LAST_CHECK_TIMESTAMP = "last_check_timestamp"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    }
}