package com.anugraha.stays.data.repository

import android.util.Log
import com.anugraha.stays.data.local.database.dao.ExternalBookingDao
import com.anugraha.stays.data.local.database.entity.ExternalBookingEntity
import com.anugraha.stays.data.remote.ical.ICalEvent
import com.anugraha.stays.data.remote.ical.ICalParser
import com.anugraha.stays.domain.model.*
import com.anugraha.stays.domain.repository.ICalSyncRepository
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
    private val externalBookingDao: ExternalBookingDao
) : ICalSyncRepository {

    override suspend fun syncICalFeeds(configs: List<ICalConfig>): NetworkResult<List<Reservation>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("ICalSync", "═══════════════════════════════════════════")
                Log.d("ICalSync", "🔄 STARTING iCAL SYNC")
                Log.d("ICalSync", "═══════════════════════════════════════════")

                val allReservations = mutableListOf<Reservation>()
                var successCount = 0
                var failureCount = 0

                configs.forEach { config ->
                    try {
                        Log.d("ICalSync", "📥 Syncing ${config.source.name}")
                        Log.d("ICalSync", "   URL: ${config.url}")

                        // ✅ ADD TIMEOUT
                        val icalContent = withTimeout(30000L) {  // 30 second timeout
                            fetchICalContent(config.url)
                        }

                        Log.d("ICalSync", "   ✅ Fetched iCal content (${icalContent.length} bytes)")

                        val events = parser.parseICalString(icalContent, config.source)
                        Log.d("ICalSync", "   ✅ Parsed ${events.size} events from ${config.source.name}")

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
                        successCount++

                        Log.d("ICalSync", "   ✅ Successfully synced ${config.source.name}")

                    } catch (e: TimeoutCancellationException) {
                        failureCount++
                        Log.e("ICalSync", "   ⏱️ Timeout syncing ${config.source.name} - skipping")
                    } catch (e: javax.net.ssl.SSLHandshakeException) {
                        failureCount++
                        Log.e("ICalSync", "   🔒 SSL error syncing ${config.source.name} - skipping")
                        Log.e("ICalSync", "   Certificate validation failed. URL may be invalid or untrusted.")
                    } catch (e: java.net.UnknownHostException) {
                        failureCount++
                        Log.e("ICalSync", "   🌐 Network error syncing ${config.source.name} - skipping")
                        Log.e("ICalSync", "   Cannot reach host. Check internet connection.")
                    } catch (e: CancellationException) {
                        failureCount++
                        Log.e("ICalSync", "   ❌ Sync cancelled for ${config.source.name}")
                        // Don't rethrow CancellationException - just skip this source
                    } catch (e: Exception) {
                        failureCount++
                        Log.e("ICalSync", "   ❌ Error syncing ${config.source.name}: ${e.message}", e)
                    }
                }

                Log.d("ICalSync", "")
                Log.d("ICalSync", "═══════════════════════════════════════════")
                Log.d("ICalSync", "✅ SYNC COMPLETE")
                Log.d("ICalSync", "   Success: $successCount source(s)")
                Log.d("ICalSync", "   Failed: $failureCount source(s)")
                Log.d("ICalSync", "   Total reservations: ${allReservations.size}")
                Log.d("ICalSync", "═══════════════════════════════════════════")

                // ✅ Return success even if some sources failed
                NetworkResult.Success(allReservations)

            } catch (e: Exception) {
                Log.e("ICalSync", "❌ FATAL ERROR: ${e.message}", e)
                NetworkResult.Error(e.message ?: "Failed to sync")
            }
        }
    }

    override suspend fun getExternalBookings(): List<Reservation> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = externalBookingDao.getAllBookings()
                entities.map { entity ->
                    createReservationFromEntity(entity)
                }
            } catch (e: Exception) {
                Log.e("ICalSync", "Error getting external bookings", e)
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
                return connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                throw Exception("HTTP error code: $responseCode")
            }
        } finally {
            connection.disconnect()
        }
    }

    // ✅ SIMPLE: Create reservation with proper display name
    private fun createReservationFromEvent(event: ICalEvent): Reservation {
        val displayName = when (event.source) {
            ICalSource.AIRBNB -> "Booking on Airbnb"
            ICalSource.BOOKING_COM -> "Booking on Booking.com"
        }

        return Reservation(
            id = event.uid.hashCode(),
            reservationNumber = "EXT-${event.source.name}-${event.uid.take(8)}",
            status = ReservationStatus.APPROVED,
            checkInDate = event.startDate,
            checkOutDate = event.endDate,
            adults = 2,
            kids = 0,
            hasPet = false,
            totalAmount = 0.0,
            primaryGuest = Guest(
                fullName = displayName,  // ✅ "Booking on Airbnb" or "Booking on Booking.com"
                phone = "",
                email = ""
            ),
            room = null,
            bookingSource = event.source.toBookingSource(),
            estimatedCheckInTime = null,
            transactionId = null,
            paymentStatus = "N/A",
            transportService = "No",
            paymentReference = null
        )
    }

    // ✅ SIMPLE: Create from database entity
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
            reservationNumber = entity.reservationNumber,
            status = ReservationStatus.APPROVED,
            checkInDate = com.anugraha.stays.util.DateUtils.parseDate(entity.checkInDate),
            checkOutDate = com.anugraha.stays.util.DateUtils.parseDate(entity.checkOutDate),
            adults = 2,
            kids = 0,
            hasPet = false,
            totalAmount = 0.0,
            primaryGuest = Guest(
                fullName = displayName,  // ✅ "Booking on Airbnb" or "Booking on Booking.com"
                phone = "",
                email = ""
            ),
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