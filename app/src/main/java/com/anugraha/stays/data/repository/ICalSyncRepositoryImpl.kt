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
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

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

                configs.forEach { config ->
                    try {
                        Log.d("ICalSync", "📥 Syncing ${config.source.name}")

                        val icalContent = fetchICalContent(config.url)
                        val events = parser.parseICalString(icalContent, config.source)
                        Log.d("ICalSync", "✅ Parsed ${events.size} events from ${config.source.name}")

                        // Convert to reservations
                        val reservations = events.map { event ->
                            createReservationFromEvent(event)
                        }

                        // Save to database
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

                        Log.d("ICalSync", "✅ Saved ${entities.size} bookings from ${config.source.name}")

                    } catch (e: Exception) {
                        Log.e("ICalSync", "❌ Error syncing ${config.source.name}: ${e.message}", e)
                    }
                }

                Log.d("ICalSync", "═══════════════════════════════════════════")
                Log.d("ICalSync", "✅ SYNC COMPLETE: ${allReservations.size} bookings")
                Log.d("ICalSync", "═══════════════════════════════════════════")

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