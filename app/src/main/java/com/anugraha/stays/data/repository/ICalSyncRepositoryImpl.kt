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
        val (result, _) = syncICalFeedsDetailed(configs)
        return result
    }

    override suspend fun syncICalFeedsDetailed(configs: List<ICalConfig>): Pair<NetworkResult<List<Reservation>>, List<com.anugraha.stays.domain.repository.SourceSyncStatus>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("ICalSync", "═══════════════════════════════════════════")
                Log.d("ICalSync", "🔄 STARTING iCAL SYNC")
                Log.d("ICalSync", "═══════════════════════════════════════════")

                val allReservations = mutableListOf<Reservation>()
                val sourceStatuses = mutableListOf<com.anugraha.stays.domain.repository.SourceSyncStatus>()
                var successCount = 0
                var failureCount = 0

                configs.forEach { config ->
                    try {
                        Log.d("ICalSync", "📥 Syncing ${config.source.name}")
                        Log.d("ICalSync", "   URL: ${config.url}")

                        val icalContent = withTimeout(30000L) {
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

                        sourceStatuses.add(
                            com.anugraha.stays.domain.repository.SourceSyncStatus(
                                source = config.source,
                                isSuccess = true
                            )
                        )

                        Log.d("ICalSync", "   ✅ Successfully synced ${config.source.name}")

                    } catch (e: TimeoutCancellationException) {
                        failureCount++
                        sourceStatuses.add(
                            com.anugraha.stays.domain.repository.SourceSyncStatus(
                                source = config.source,
                                isSuccess = false,
                                errorMessage = "Timeout - server did not respond"
                            )
                        )
                        Log.e("ICalSync", "   ⏱️ Timeout syncing ${config.source.name} - skipping")
                    } catch (e: javax.net.ssl.SSLHandshakeException) {
                        failureCount++
                        sourceStatuses.add(
                            com.anugraha.stays.domain.repository.SourceSyncStatus(
                                source = config.source,
                                isSuccess = false,
                                errorMessage = "SSL certificate error"
                            )
                        )
                        Log.e("ICalSync", "   🔒 SSL error syncing ${config.source.name} - skipping")
                    } catch (e: java.net.UnknownHostException) {
                        failureCount++
                        sourceStatuses.add(
                            com.anugraha.stays.domain.repository.SourceSyncStatus(
                                source = config.source,
                                isSuccess = false,
                                errorMessage = "Network error - cannot reach server"
                            )
                        )
                        Log.e("ICalSync", "   🌐 Network error syncing ${config.source.name} - skipping")
                    } catch (e: CancellationException) {
                        failureCount++
                        sourceStatuses.add(
                            com.anugraha.stays.domain.repository.SourceSyncStatus(
                                source = config.source,
                                isSuccess = false,
                                errorMessage = "Sync cancelled"
                            )
                        )
                        Log.e("ICalSync", "   ❌ Sync cancelled for ${config.source.name}")
                    } catch (e: Exception) {
                        failureCount++
                        sourceStatuses.add(
                            com.anugraha.stays.domain.repository.SourceSyncStatus(
                                source = config.source,
                                isSuccess = false,
                                errorMessage = e.message ?: "Unknown error"
                            )
                        )
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

                Pair(NetworkResult.Success(allReservations), sourceStatuses)

            } catch (e: Exception) {
                Log.e("ICalSync", "❌ FATAL ERROR: ${e.message}", e)
                Pair(
                    NetworkResult.Error(e.message ?: "Failed to sync"),
                    emptyList()
                )
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