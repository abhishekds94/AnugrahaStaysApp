package com.anugraha.stays.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.anugraha.stays.MainActivity
import com.anugraha.stays.R
import com.anugraha.stays.domain.model.Reservation

object NotificationHelper {

    private const val CHANNEL_ID = "booking_requests"
    private const val CHANNEL_NAME = "Booking Requests"
    private const val CHANNEL_DESCRIPTION = "Notifications for new booking requests"

    /**
     * Create notification channel (required for Android O and above)
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Show notification for new booking request
     */
    fun showNewBookingNotification(
        context: Context,
        reservation: Reservation,
        count: Int = 1
    ) {
        // Check notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "pending_details")
            putExtra("reservation_id", reservation.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            reservation.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (count == 1) {
            "New Booking Request"
        } else {
            "$count New Booking Requests"
        }

        val message = if (count == 1) {
            "New booking from ${reservation.primaryGuest.fullName}\n" +
                    "Check-in: ${reservation.checkInDate}\n" +
                    "Guests: ${reservation.adults + reservation.kids}"
        } else {
            "You have $count new booking requests waiting for approval"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(reservation.id, notification)
    }

    /**
     * Show summary notification for multiple new bookings
     */
    fun showMultipleBookingsNotification(
        context: Context,
        reservations: List<Reservation>
    ) {
        if (reservations.isEmpty()) return

        // Check notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "reservations")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle("${reservations.size} New Booking Requests")
            .setSummaryText("Tap to review")

        reservations.take(5).forEach { reservation ->
            inboxStyle.addLine("${reservation.primaryGuest.fullName} - ${reservation.checkInDate}")
        }

        if (reservations.size > 5) {
            inboxStyle.addLine("+ ${reservations.size - 5} more...")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("${reservations.size} New Booking Requests")
            .setContentText("You have pending booking requests to review")
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setNumber(reservations.size)
            .build()

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(NOTIFICATION_ID_SUMMARY, notification)
    }

    /**
     * Cancel all booking notifications
     */
    fun cancelAllBookingNotifications(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(NOTIFICATION_ID_SUMMARY)
    }

    private const val NOTIFICATION_ID_SUMMARY = 1000
}