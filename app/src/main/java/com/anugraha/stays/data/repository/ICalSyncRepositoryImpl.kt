package com.anugraha.stays.data.repository

import com.anugraha.stays.data.local.database.dao.ExternalBookingDao
import com.anugraha.stays.data.local.database.entity.ExternalBookingEntity
import com.anugraha.stays.data.remote.ical.ICalEvent
import com.anugraha.stays.data.remote.ical.ICalParser
import com.anugraha.stays.domain.model.*
import com.anugraha.stays.domain.repository.ICalSyncRepository
import com.anugraha.stays.domain.repository.SourceSyncStatus
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
    private val externalBookingDao: ExternalBookingDao
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
                        val icalContent = withTimeout(30000L) {
                            fetchICalContent(config.url)
                        }

                        val events = parser.parseICalString(icalContent, config.source)
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
                        sourceStatuses.add(SourceSyncStatus(source = config.source, isSuccess = false, errorMessage = "Timeout"))
                    } catch (e: Exception) {
                        sourceStatuses.add(SourceSyncStatus(source = config.source, isSuccess = false, errorMessage = e.message ?: "Unknown error"))
                    }
                }

                Pair(NetworkResult.Success(allReservations), sourceStatuses)
            } catch (e: Exception) {
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
                return connection.inputStream.bufferedReader().use { it.readText() }
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
            reservationNumber = "EXT-${event.source.name}-${event.uid.take(8)}",
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
            reservationNumber = entity.reservationNumber,
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
