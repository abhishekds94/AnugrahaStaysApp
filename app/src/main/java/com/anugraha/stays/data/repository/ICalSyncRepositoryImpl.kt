package com.anugraha.stays.data.repository

import com.anugraha.stays.data.local.database.dao.ExternalBookingDao
import com.anugraha.stays.data.local.database.entity.ExternalBookingEntity
import com.anugraha.stays.data.remote.ical.ICalEvent
import com.anugraha.stays.data.remote.ical.ICalParser
import com.anugraha.stays.domain.model.*
import com.anugraha.stays.domain.repository.ICalSyncRepository
import com.anugraha.stays.domain.repository.SourceSyncStatus
import com.anugraha.stays.util.AdvancedBookingDeduplicator
import com.anugraha.stays.util.DateUtils
import com.anugraha.stays.util.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class ICalSyncRepositoryImpl @Inject constructor(
    private val parser: ICalParser,
    private val externalBookingDao: ExternalBookingDao,
    private val advancedDeduplicator: AdvancedBookingDeduplicator
) : ICalSyncRepository {

    override suspend fun syncICalFeeds(configs: List<ICalConfig>): NetworkResult<List<Reservation>> {
        val (result, _) = syncICalFeedsDetailed(configs)
        return result
    }

    override suspend fun syncICalFeedsDetailed(configs: List<ICalConfig>): Pair<NetworkResult<List<Reservation>>, List<SourceSyncStatus>> {
        return withContext(Dispatchers.IO) {
            try {
                val allReservations = mutableListOf<Reservation>()
                val sourceStatuses = mutableListOf<SourceSyncStatus>()

                configs.forEach { config ->
                    try {
                        android.util.Log.d("ICalSync", "========================================")
                        android.util.Log.d("ICalSync", "Fetching ${config.source.name} calendar")
                        android.util.Log.d("ICalSync", "URL: ${config.url}")
                        android.util.Log.d("ICalSync", "========================================")

                        val icalContent = withTimeout(30000L) {
                            fetchICalContent(config.url)
                        }

                        // LOG FULL RESPONSE
                        android.util.Log.d("ICalSync", "========================================")
                        android.util.Log.d("ICalSync", "FULL ${config.source.name} RESPONSE:")
                        android.util.Log.d("ICalSync", "========================================")
                        android.util.Log.d("ICalSync", icalContent)
                        android.util.Log.d("ICalSync", "========================================")
                        android.util.Log.d("ICalSync", "END OF ${config.source.name} RESPONSE")
                        android.util.Log.d("ICalSync", "========================================")

                        val events = parser.parseICalString(icalContent, config.source)

                        android.util.Log.d("ICalSync", "${config.source.name}: Parsed ${events.size} events")
                        events.forEachIndexed { index, event ->
                            android.util.Log.d("ICalSync", "  Event #${index + 1}:")
                            android.util.Log.d("ICalSync", "    UID: ${event.uid}")
                            android.util.Log.d("ICalSync", "    SUMMARY: ${event.summary}")
                            android.util.Log.d("ICalSync", "    START: ${event.startDate}")
                            android.util.Log.d("ICalSync", "    END: ${event.endDate}")
                        }

                        val reservations = events.map { createReservationFromEvent(it) }

                        val entities = events.map { event ->
                            ExternalBookingEntity(
                                uid = event.uid,
                                reservationNumber = "EXT-${event.source.name}-${event.uid.take(8)}",
                                source = event.source.name,
                                summary = event.summary,
                                checkInDate = event.startDate.toString(),
                                checkOutDate = event.endDate.toString()
                            )
                        }

                        externalBookingDao.deleteBySource(config.source.name)
                        externalBookingDao.insertAll(entities)

                        allReservations.addAll(reservations)
                        sourceStatuses.add(SourceSyncStatus(source = config.source, isSuccess = true))

                    } catch (e: TimeoutCancellationException) {
                        android.util.Log.e("ICalSync", "${config.source.name} sync failed: Timeout", e)
                        sourceStatuses.add(SourceSyncStatus(source = config.source, isSuccess = false, errorMessage = "Timeout"))
                    } catch (e: Exception) {
                        android.util.Log.e("ICalSync", "${config.source.name} sync failed: ${e.message}", e)
                        sourceStatuses.add(SourceSyncStatus(source = config.source, isSuccess = false, errorMessage = e.message ?: "Unknown error"))
                    }
                }

                // Note: Deduplication is now done in ReservationsViewModel
                // using AdvancedBookingDeduplicator which handles both
                // cross-platform duplicates AND direct booking conflicts
                android.util.Log.d("ICalSync", "========================================")
                android.util.Log.d("ICalSync", "iCal sync complete")
                android.util.Log.d("ICalSync", "Total external bookings synced: ${allReservations.size}")
                android.util.Log.d("ICalSync", "========================================")

                Pair(NetworkResult.Success(allReservations), sourceStatuses)
            } catch (e: Exception) {
                android.util.Log.e("ICalSync", "Sync failed completely: ${e.message}", e)
                Pair(NetworkResult.Error(e.message ?: "Failed to sync"), emptyList())
            }
        }
    }

    override suspend fun getExternalBookings(): List<Reservation> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = externalBookingDao.getAllBookings()
                entities.map { createReservationFromEntity(it) }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private fun fetchICalContent(urlString: String): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 30000
            connection.readTimeout = 30000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val content = connection.inputStream.bufferedReader().use { it.readText() }

                // Save to file for debugging
                try {
                    val source = if (urlString.contains("airbnb")) "airbnb" else "booking"
                    val file = java.io.File("/sdcard/Download/${source}_ical_response.txt")
                    file.writeText("URL: $urlString\n\n")
                    file.appendText("Timestamp: ${System.currentTimeMillis()}\n\n")
                    file.appendText("=".repeat(80) + "\n")
                    file.appendText("FULL ICAL RESPONSE:\n")
                    file.appendText("=".repeat(80) + "\n\n")
                    file.appendText(content)
                    android.util.Log.d("ICalSync", "✅ Saved $source response to: ${file.absolutePath}")
                } catch (e: Exception) {
                    android.util.Log.w("ICalSync", "Could not save to file: ${e.message}")
                }

                return content
            } else {
                throw Exception("HTTP error code: $responseCode")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun createReservationFromEvent(event: ICalEvent): Reservation {
        val displayName = when (event.source) {
            ICalSource.AIRBNB -> "Booking on Airbnb"
            ICalSource.BOOKING_COM -> "Booking on Booking.com"
        }

        return Reservation(
            id = event.uid.hashCode(),
            reservationNumber = event.summary, // Store actual summary for deduplication
            status = ReservationStatus.APPROVED,
            checkInDate = event.startDate,
            checkOutDate = event.endDate,
            adults = 2,
            kids = 0,
            hasPet = false,
            totalAmount = 0.0,
            primaryGuest = Guest(fullName = displayName, phone = "", email = ""),
            room = null,
            bookingSource = event.source.toBookingSource(),
            estimatedCheckInTime = null,
            transactionId = null,
            paymentStatus = "N/A",
            transportService = "No",
            paymentReference = null
        )
    }

    private fun createReservationFromEntity(entity: ExternalBookingEntity): Reservation {
        val source = when (entity.source) {
            "AIRBNB" -> ICalSource.AIRBNB
            "BOOKING_COM" -> ICalSource.BOOKING_COM
            else -> ICalSource.AIRBNB
        }

        val displayName = when (source) {
            ICalSource.AIRBNB -> "Booking on Airbnb"
            ICalSource.BOOKING_COM -> "Booking on Booking.com"
        }

        return Reservation(
            id = entity.uid.hashCode(),
            reservationNumber = entity.summary, // Use actual summary for deduplication
            status = ReservationStatus.APPROVED,
            checkInDate = DateUtils.parseDate(entity.checkInDate),
            checkOutDate = DateUtils.parseDate(entity.checkOutDate),
            adults = 2,
            kids = 0,
            hasPet = false,
            totalAmount = 0.0,
            primaryGuest = Guest(fullName = displayName, phone = "", email = ""),
            room = null,
            bookingSource = source.toBookingSource(),
            estimatedCheckInTime = null,
            transactionId = null,
            paymentStatus = "N/A",
            transportService = "No",
            paymentReference = null
        )
    }
}