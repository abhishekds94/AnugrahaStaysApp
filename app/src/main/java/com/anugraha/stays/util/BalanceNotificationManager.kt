package com.anugraha.stays.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.anugraha.stays.R
import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.domain.model.getPendingBalance
import com.anugraha.stays.domain.model.hasPendingBalance
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages balance notifications and alerts using WorkManager (safer alternative)
 * Uses WorkManager instead of AlarmManager - no special permissions required
 */
@Singleton
class BalanceNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val CHANNEL_ID = "balance_reminders"
        private const val CHANNEL_NAME = "Balance Reminders"
        private const val CHANNEL_DESCRIPTION = "Notifications for pending balance reminders"
        private const val NOTIFICATION_HOUR = 8 // 8:00 AM
        private const val NOTIFICATION_MINUTE = 0

        // Preference keys for tracking shown popups
        private const val PREFS_NAME = "balance_popups"
        private const val POPUP_SHOWN_PREFIX = "popup_shown_"

        // WorkManager tags
        private const val WORK_TAG_PREFIX = "balance_notification_"
    }

    init {
        createNotificationChannel()
    }

    /**
     * Create notification channel (required for Android O+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Schedule notifications for bookings with pending balances using WorkManager
     * This is safer than AlarmManager and doesn't require special permissions
     */
    fun scheduleBalanceNotifications(reservations: List<Reservation>) {
        val today = LocalDate.now()
        val workManager = WorkManager.getInstance(context)

        reservations.forEach { reservation ->
            if (reservation.hasPendingBalance() && reservation.getPendingBalance() > 0) {
                // Schedule for check-in day
                if (reservation.checkInDate >= today) {
                    scheduleNotificationWork(
                        reservation = reservation,
                        date = reservation.checkInDate,
                        workManager = workManager,
                        type = "checkin"
                    )
                }

                // Schedule for check-out day
                if (reservation.checkOutDate >= today) {
                    scheduleNotificationWork(
                        reservation = reservation,
                        date = reservation.checkOutDate,
                        workManager = workManager,
                        type = "checkout"
                    )
                }
            }
        }
    }

    /**
     * Schedule a notification using WorkManager (no special permissions needed)
     */
    private fun scheduleNotificationWork(
        reservation: Reservation,
        date: LocalDate,
        workManager: WorkManager,
        type: String
    ) {
        val notificationTime = LocalDateTime.of(date, LocalTime.of(NOTIFICATION_HOUR, NOTIFICATION_MINUTE))
        val now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"))

        // Calculate delay
        val delayMillis = Duration.between(now, notificationTime.atZone(ZoneId.of("Asia/Kolkata"))).toMillis()

        // Only schedule if time is in the future
        if (delayMillis > 0) {
            // Create input data
            val inputData = workDataOf(
                "reservation_id" to reservation.id,
                "guest_name" to reservation.primaryGuest.fullName,
                "pending_balance" to reservation.getPendingBalance(),
                "type" to type
            )

            // Create work request with exact timing constraints
            val workRequest = OneTimeWorkRequestBuilder<BalanceNotificationWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag("${WORK_TAG_PREFIX}${reservation.id}_${type}_${date}")
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false) // Run even on low battery
                        .build()
                )
                .build()

            // Enqueue the work
            workManager.enqueueUniqueWork(
                "${WORK_TAG_PREFIX}${reservation.id}_${type}_${date}",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }

    /**
     * Cancel all scheduled notifications for a reservation
     */
    fun cancelNotificationsForReservation(reservationId: Int) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag("${WORK_TAG_PREFIX}${reservationId}")
    }

    /**
     * Show immediate notification (for testing or immediate alerts)
     */
    fun showBalanceNotification(
        reservationId: Int,
        guestName: String,
        pendingBalance: Double
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Pending Balance Reminder")
            .setContentText("$guestName has a pending balance of ₹${String.format("%.0f", pendingBalance)}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(reservationId, notification)
    }

    /**
     * Check if popup should be shown for a booking on a specific date
     * Returns true if popup hasn't been shown today for this booking
     */
    fun shouldShowPopup(reservationId: Int, date: LocalDate): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "$POPUP_SHOWN_PREFIX${reservationId}_$date"
        val lastShown = prefs.getString(key, "")
        val today = LocalDate.now().toString()

        return lastShown != today
    }

    /**
     * Mark popup as shown for a booking on a specific date
     */
    fun markPopupShown(reservationId: Int, date: LocalDate) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "$POPUP_SHOWN_PREFIX${reservationId}_$date"
        val today = LocalDate.now().toString()

        prefs.edit().putString(key, today).apply()
    }

    /**
     * Get list of bookings that need popup today
     */
    fun getBookingsNeedingPopup(reservations: List<Reservation>): List<Reservation> {
        val today = LocalDate.now()

        return reservations.filter { reservation ->
            val hasPendingBalance = reservation.hasPendingBalance() && reservation.getPendingBalance() > 0
            val isCheckInToday = reservation.checkInDate == today
            val isCheckOutToday = reservation.checkOutDate == today
            val shouldShow = shouldShowPopup(reservation.id, today)

            hasPendingBalance && (isCheckInToday || isCheckOutToday) && shouldShow
        }
    }
}

/**
 * Worker for showing balance notifications
 * Runs in the background using WorkManager
 */
class BalanceNotificationWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val reservationId = inputData.getInt("reservation_id", 0)
        val guestName = inputData.getString("guest_name") ?: "Guest"
        val pendingBalance = inputData.getDouble("pending_balance", 0.0)

        if (reservationId != 0 && pendingBalance > 0) {
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val notification = NotificationCompat.Builder(applicationContext, "balance_reminders")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Pending Balance Reminder")
                .setContentText("$guestName has a pending balance of ₹${String.format("%.0f", pendingBalance)}")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(reservationId, notification)

            return Result.success()
        }

        return Result.failure()
    }
}