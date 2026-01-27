package com.anugraha.stays

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anugraha.stays.data.local.preferences.BookingNotificationPreferences
import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.domain.model.ReservationStatus
import com.anugraha.stays.domain.repository.DashboardRepository
import com.anugraha.stays.util.NetworkResult
import com.anugraha.stays.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background worker that periodically checks for new booking requests
 * Uses WorkManager for efficient, battery-friendly background execution
 */
@HiltWorker
class BookingCheckWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val dashboardRepository: DashboardRepository,
    private val notificationPrefs: BookingNotificationPreferences
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Check if notifications are enabled
            if (!notificationPrefs.notificationsEnabled) {
                return@withContext Result.success()
            }

            // Fetch pending reservations
            when (val result = dashboardRepository.getPendingReservations()) {
                is NetworkResult.Success -> {
                    val pendingReservations = result.data.filter {
                        it.status == ReservationStatus.PENDING
                    }

                    // Get previously seen reservation IDs
                    val seenIds = notificationPrefs.getSeenReservationIds()

                    // Find new (unseen) reservations
                    val newReservations = pendingReservations.filter { reservation ->
                        !seenIds.contains(reservation.id)
                    }

                    // Show notifications for new reservations
                    if (newReservations.isNotEmpty()) {
                        showNotifications(newReservations)

                        // Mark these reservations as seen
                        notificationPrefs.markReservationsAsSeen(newReservations.map { it.id })
                    }

                    // Cleanup old seen reservations to prevent set from growing indefinitely
                    notificationPrefs.cleanupOldSeenReservations(
                        pendingReservations.map { it.id }
                    )

                    // Update last check timestamp
                    notificationPrefs.lastCheckTimestamp = System.currentTimeMillis()

                    Result.success()
                }
                is NetworkResult.Error -> {
                    // Retry on network error
                    Result.retry()
                }
                NetworkResult.Loading -> {
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun showNotifications(newReservations: List<Reservation>) {
        when {
            newReservations.size == 1 -> {
                // Show single notification
                NotificationHelper.showNewBookingNotification(
                    context,
                    newReservations.first(),
                    count = 1
                )
            }
            newReservations.size > 1 -> {
                // Show grouped notification
                NotificationHelper.showMultipleBookingsNotification(
                    context,
                    newReservations
                )
            }
        }
    }

    companion object {
        const val WORK_NAME = "booking_check_worker"
    }
}