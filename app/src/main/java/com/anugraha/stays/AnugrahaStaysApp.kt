package com.anugraha.stays

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.anugraha.stays.util.BookingNotificationWorkManager
import com.anugraha.stays.util.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AnugrahaStaysApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var bookingNotificationWorkManager: BookingNotificationWorkManager

    override fun onCreate() {
        super.onCreate()

        // Create notification channel
        NotificationHelper.createNotificationChannel(this)

        // Schedule periodic booking checks at 9 AM and 9 PM IST
        bookingNotificationWorkManager.scheduleBookingChecks()
    }

    override fun getWorkManagerConfiguration(): Configuration =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}