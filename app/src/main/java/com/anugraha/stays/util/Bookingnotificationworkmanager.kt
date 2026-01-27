package com.anugraha.stays.util

import android.content.Context
import androidx.work.*
import com.anugraha.stays.BookingCheckWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages WorkManager scheduling for booking notifications
 * Schedules checks at 9 AM IST and 9 PM IST daily
 */
@Singleton
class BookingNotificationWorkManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val workManager = WorkManager.getInstance(context)

    /**
     * Schedule booking checks at 9 AM and 9 PM IST daily
     */
    fun scheduleBookingChecks() {
        // Cancel any existing work first
        cancelBookingChecks()

        // Schedule 9 AM IST check
        scheduleDailyCheckAt(HOUR_9AM_IST, WORK_NAME_9AM)

        // Schedule 9 PM IST check
        scheduleDailyCheckAt(HOUR_9PM_IST, WORK_NAME_9PM)
    }

    /**
     * Schedule a daily check at specific hour (IST)
     */
    private fun scheduleDailyCheckAt(targetHour: Int, workName: String) {
        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If target time has passed today, schedule for tomorrow
            if (before(currentTime)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val initialDelayMinutes = (targetTime.timeInMillis - currentTime.timeInMillis) / 60000

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<BookingCheckWorker>(
            24, // Repeat every 24 hours
            TimeUnit.HOURS,
            30, // Flex window of 30 minutes
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag(TAG_BOOKING_CHECK)
            .build()

        workManager.enqueueUniquePeriodicWork(
            workName,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * Trigger immediate one-time check
     * Useful when app is opened or after accepting/declining a booking
     */
    fun triggerImmediateCheck() {
        // Check if we're within the allowed time windows (9 AM or 9 PM IST ± 30 minutes)
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val currentMinute = Calendar.getInstance().get(Calendar.MINUTE)

        val is9AMWindow = (currentHour == 9 && currentMinute <= 30) || (currentHour == 8 && currentMinute >= 30)
        val is9PMWindow = (currentHour == 21 && currentMinute <= 30) || (currentHour == 20 && currentMinute >= 30)

        // Only trigger immediate check if we're in a valid time window or if forced
        // For now, we'll allow immediate checks anytime (can be restricted if needed)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<BookingCheckWorker>()
            .setConstraints(constraints)
            .addTag(TAG_BOOKING_CHECK)
            .build()

        workManager.enqueueUniqueWork(
            "${BookingCheckWorker.WORK_NAME}_immediate",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    /**
     * Cancel all booking check work
     */
    fun cancelBookingChecks() {
        workManager.cancelUniqueWork(WORK_NAME_9AM)
        workManager.cancelUniqueWork(WORK_NAME_9PM)
        workManager.cancelAllWorkByTag(TAG_BOOKING_CHECK)
    }

    /**
     * Get work info to check if workers are running
     */
    fun getWorkInfo9AM() = workManager.getWorkInfosForUniqueWorkLiveData(WORK_NAME_9AM)
    fun getWorkInfo9PM() = workManager.getWorkInfosForUniqueWorkLiveData(WORK_NAME_9PM)

    companion object {
        private const val HOUR_9AM_IST = 9   // 9 AM IST
        private const val HOUR_9PM_IST = 21  // 9 PM IST (21:00 in 24-hour format)

        private const val WORK_NAME_9AM = "booking_check_9am"
        private const val WORK_NAME_9PM = "booking_check_9pm"
        private const val TAG_BOOKING_CHECK = "booking_check"
    }
}