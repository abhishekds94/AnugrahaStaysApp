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
                        Log.d("ICalSync", "")
                        Log.d("ICalSync", "📥 Syncing ${config.source.name}")
                        Log.d("ICalSync", "   URL: ${config.url}")

                        val icalContent = fetchICalContent(config.url)
                        Log.d("ICalSync", "   ✅ Fetched iCal content (${icalContent.length} bytes)")

                        val events = parser.parseICalString(icalContent, config.source)
                        Log.d("ICalSync", "   ✅ Parsed ${events.size} events from ${config.source.name}")
                        Log.d("ICalSync", "")

                        // LOG EACH EVENT IN DETAIL
                        events.forEachIndexed { index, event ->
                            Log.d("ICalSync", "   📅 Event #${index + 1}:")
                            Log.d("ICalSync", "      UID: ${event.uid}")
                            Log.d("ICalSync", "      Summary: ${event.summary}")
                            Log.d("ICalSync", "      Check-in: ${event.startDate}")
                            Log.d("ICalSync", "      Check-out: ${event.endDate}")
                            Log.d("ICalSync", "      Nights: ${java.time.temporal.ChronoUnit.DAYS.between(event.startDate, event.endDate)}")
                            Log.d("ICalSync", "      Source: ${event.source.name}")
                        }

                        val reservations = events.map { it.toReservation() }

                        // LOG CONVERTED RESERVATIONS
                        Log.d("ICalSync", "")
                        Log.d("ICalSync", "   🔄 Converting to Reservation objects...")
                        reservations.forEachIndexed { index, res ->
                            Log.d("ICalSync", "   🏨 Reservation #${index + 1}:")
                            Log.d("ICalSync", "      ID: ${res.id}")
                            Log.d("ICalSync", "      Number: ${res.reservationNumber}")
                            Log.d("ICalSync", "      Guest: ${res.primaryGuest.fullName}")
                            Log.d("ICalSync", "      Source: ${res.bookingSource.displayName()}")
                            Log.d("ICalSync", "      Check-in: ${res.checkInDate}")
                            Log.d("ICalSync", "      Check-out: ${res.checkOutDate}")
                            Log.d("ICalSync", "      Status: ${res.status}")
                        }

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

                        Log.d("ICalSync", "")
                        Log.d("ICalSync", "   💾 Saving to database...")
                        Log.d("ICalSync", "   🗑️ Deleting old ${config.source.name} bookings...")
                        externalBookingDao.deleteBySource(config.source.name)

                        Log.d("ICalSync", "   💾 Inserting ${entities.size} new bookings...")
                        externalBookingDao.insertAll(entities)

                        Log.d("ICalSync", "   ✅ Database updated successfully")

                        allReservations.addAll(reservations)

                    } catch (e: Exception) {
                        Log.e("ICalSync", "   ❌ ERROR syncing ${config.source.name}: ${e.message}", e)
                        Log.e("ICalSync", "   Stack trace:", e)
                    }
                }

                Log.d("ICalSync", "")
                Log.d("ICalSync", "═══════════════════════════════════════════")
                Log.d("ICalSync", "✅ SYNC COMPLETE")
                Log.d("ICalSync", "   Total reservations synced: ${allReservations.size}")
                Log.d("ICalSync", "")

                // LOG SUMMARY BY SOURCE
                val airbnbCount = allReservations.count { it.bookingSource == BookingSource.AIRBNB }
                val bookingComCount = allReservations.count { it.bookingSource == BookingSource.BOOKING_COM }

                Log.d("ICalSync", "   📊 SUMMARY:")
                Log.d("ICalSync", "      Airbnb: $airbnbCount bookings")
                Log.d("ICalSync", "      Booking.com: $bookingComCount bookings")
                Log.d("ICalSync", "")

                // LOG DATE RANGES
                if (allReservations.isNotEmpty()) {
                    val sortedByDate = allReservations.sortedBy { it.checkInDate }
                    Log.d("ICalSync", "   📅 DATE RANGE:")
                    Log.d("ICalSync", "      First check-in: ${sortedByDate.first().checkInDate}")
                    Log.d("ICalSync", "      Last check-out: ${sortedByDate.last().checkOutDate}")
                }

                Log.d("ICalSync", "═══════════════════════════════════════════")

                NetworkResult.Success(allReservations)
            } catch (e: Exception) {
                Log.e("ICalSync", "═══════════════════════════════════════════")
                Log.e("ICalSync", "❌ FATAL ERROR in syncICalFeeds", e)
                Log.e("ICalSync", "   Message: ${e.message}")
                Log.e("ICalSync", "═══════════════════════════════════════════")
                NetworkResult.Error(e.message ?: "Failed to sync iCal feeds")
            }
        }
    }

    override suspend fun getExternalBookings(): List<Reservation> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = externalBookingDao.getAllBookings()
                entities.map { it.toDomain() }
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

    private fun ICalEvent.toReservation(): Reservation {
        return Reservation(
            id = uid.hashCode(),
            reservationNumber = "EXT-${source.name}-${uid.take(8)}",
            status = ReservationStatus.APPROVED,
            checkInDate = startDate,
            checkOutDate = endDate,
            adults = 2,
            kids = 0,
            hasPet = false,
            totalAmount = 0.0,
            primaryGuest = Guest(
                fullName = source.getDisplayName(),
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