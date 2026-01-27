package com.anugraha.stays.data.repository

import com.anugraha.stays.BuildConfig
import com.anugraha.stays.data.remote.api.AnugrahaApi
import com.anugraha.stays.data.remote.dto.ReservationDto
import com.anugraha.stays.data.remote.dto.toDomain
import com.anugraha.stays.domain.model.*
import com.anugraha.stays.domain.repository.DashboardRepository
import com.anugraha.stays.domain.repository.ICalSyncRepository
import com.anugraha.stays.util.BalanceTestDataGenerator
import com.anugraha.stays.util.DateUtils
import com.anugraha.stays.util.NetworkResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.LocalTime
import javax.inject.Inject

class DashboardRepositoryImpl @Inject constructor(
    private val api: AnugrahaApi,
    private val iCalSyncRepository: ICalSyncRepository
) : DashboardRepository {

    override suspend fun getTodayCheckIns(): NetworkResult<List<CheckIn>> = coroutineScope {
        try {
            // DEBUG MODE: Inject test data for March 2027
            if (BuildConfig.DEBUG && isTestingMode()) {
                return@coroutineScope getDebugTodayCheckIns()
            }

            val response = api.getTodayCheckIns()
            if (!response.isSuccessful) return@coroutineScope NetworkResult.Error("API_ERROR_${response.code()}")

            val reservationDtos = response.body()?.data ?: emptyList()

            val apiCheckIns = reservationDtos.map { dto ->
                async { fetchFullReservationAndMap(dto) }
            }.awaitAll()
                .filterNotNull()
                .filter { reservation ->
                    // Exclude cancelled bookings
                    reservation.status.name.lowercase() !in listOf(
                        "pending", "declined", "cancelled", "admin_cancelled"
                    )
                }
                .map { CheckIn(it, it.checkInDate.atStartOfDay().toLocalTime()) }

            val externalBookings = iCalSyncRepository.getExternalBookings()
            val today = DateUtils.now()

            val externalCheckIns = externalBookings
                .filter { it.checkInDate == today }
                .map { CheckIn(reservation = it, checkInTime = null) }

            NetworkResult.Success(apiCheckIns + externalCheckIns)
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "UNKNOWN_ERROR")
        }
    }

    override suspend fun getTodayCheckOuts(): NetworkResult<List<CheckOut>> = coroutineScope {
        try {
            // DEBUG MODE: Inject test data for March 2027
            if (BuildConfig.DEBUG && isTestingMode()) {
                return@coroutineScope getDebugTodayCheckOuts()
            }

            val today = DateUtils.now()

            // Get all reservations without status filter (API doesn't support multiple statuses)
            val response = api.getReservations(
                perPage = 100,
                sortBy = "check_out_date",
                sortOrder = "asc"
            )

            if (!response.isSuccessful) return@coroutineScope NetworkResult.Error("API_ERROR_${response.code()}")

            val reservationDtos = response.body() ?: emptyList()

            // Filter for today's checkouts with active statuses
            val apiCheckOuts = reservationDtos
                .map { async { fetchFullReservationAndMap(it) } }
                .awaitAll()
                .filterNotNull()
                .filter { reservation ->
                    // Check if checkout date is today and status is active
                    val isToday = reservation.checkOutDate.isEqual(today)
                    val isActiveStatus = reservation.status.name.lowercase() !in listOf(
                        "pending", "declined", "cancelled", "admin_cancelled"
                    )
                    isToday && isActiveStatus
                }
                .map { CheckOut(it, LocalTime.of(11, 0)) }

            // Add external bookings (Airbnb, Booking.com, etc.)
            val externalBookings = iCalSyncRepository.getExternalBookings()
            val externalCheckOuts = externalBookings
                .filter { it.checkOutDate == today }
                .map { CheckOut(it, LocalTime.of(11, 0)) }

            NetworkResult.Success(apiCheckOuts + externalCheckOuts)
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "UNKNOWN_ERROR")
        }
    }

    override suspend fun getThisWeekBookings(): NetworkResult<List<WeekBooking>> = coroutineScope {
        try {
            // DEBUG MODE: Inject test data for March 2027
            if (BuildConfig.DEBUG && isTestingMode()) {
                return@coroutineScope getDebugWeekBookings()
            }

            val (weekStart, weekEnd) = DateUtils.getCurrentWeekDates()
            val today = DateUtils.now()

            // Get all reservations without status filter (API doesn't support multiple statuses)
            val response = api.getReservations(
                perPage = 100,
                sortBy = "check_in_date",
                sortOrder = "asc"
            )

            if (!response.isSuccessful) return@coroutineScope NetworkResult.Error("API_ERROR_${response.code()}")

            val apiWeekBookings = (response.body() ?: emptyList())
                .map { async { fetchFullReservationAndMap(it) } }
                .awaitAll()
                .filterNotNull()
                .filter { reservation ->
                    // Check if check-in is within this week and not in the past
                    val checkInDate = reservation.checkInDate
                    val isThisWeek = !checkInDate.isBefore(weekStart) &&
                            !checkInDate.isAfter(weekEnd) &&
                            !checkInDate.isBefore(today)
                    val isActiveStatus = reservation.status.name.lowercase() !in listOf(
                        "pending", "declined", "cancelled", "admin_cancelled"
                    )
                    isThisWeek && isActiveStatus
                }
                .map { WeekBooking(it, it.checkInDate.dayOfWeek, it.checkInDate) }

            // Add external bookings (Airbnb, Booking.com, etc.)
            val externalWeekBookings = iCalSyncRepository.getExternalBookings()
                .filter { reservation ->
                    val date = reservation.checkInDate
                    !date.isBefore(weekStart) && !date.isAfter(weekEnd) && !date.isBefore(today)
                }
                .map { WeekBooking(it, it.checkInDate.dayOfWeek, it.checkInDate) }

            val allSorted = (apiWeekBookings + externalWeekBookings).sortedBy { it.date }
            NetworkResult.Success(allSorted)
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "UNKNOWN_ERROR")
        }
    }

    override suspend fun getPendingReservations(): NetworkResult<List<Reservation>> = coroutineScope {
        try {
            val response = api.getReservations(
                status = "pending",
                perPage = 50,
                sortBy = "check_in_date",
                sortOrder = "asc"
            )

            if (!response.isSuccessful) return@coroutineScope NetworkResult.Error("API_ERROR_${response.code()}")

            val reservations = (response.body() ?: emptyList())
                .map { async { fetchFullReservationAndMap(it) } }
                .awaitAll()
                .filterNotNull()

            NetworkResult.Success(reservations)
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "UNKNOWN_ERROR")
        }
    }

    private suspend fun fetchFullReservationAndMap(incompleteDto: ReservationDto): Reservation? {
        val id = incompleteDto.id ?: return incompleteDto.toDomain()
        return try {
            val response = api.getReservationById(id)
            if (response.isSuccessful) {
                response.body()?.toDomain()
            } else {
                incompleteDto.toDomain()
            }
        } catch (e: Exception) {
            incompleteDto.toDomain()
        }
    }

    /**
     * Check if we're in testing mode (March 2027)
     */
    private fun isTestingMode(): Boolean {
        val today = DateUtils.now()
        return today.year == 2027 && today.monthValue == 3
    }

    /**
     * Get debug check-ins for March 2027
     */
    private fun getDebugTodayCheckIns(): NetworkResult<List<CheckIn>> {
        val today = DateUtils.now()
        val testReservations = BalanceTestDataGenerator.generateTestReservations()

        val todayCheckIns = testReservations
            .filter { it.checkInDate == today }
            .filter { it.status.name.lowercase() !in listOf("pending", "declined", "cancelled", "admin_cancelled") }
            .map { CheckIn(it, LocalTime.of(14, 0)) }

        android.util.Log.d("DEBUG_DATA", "========== DEBUG MODE ACTIVE ==========")
        android.util.Log.d("DEBUG_DATA", "Generated ${todayCheckIns.size} test check-ins for $today")
        todayCheckIns.forEach { checkIn ->
            android.util.Log.d("DEBUG_DATA", "  - ${checkIn.reservation.primaryGuest.fullName} (${checkIn.reservation.reservationNumber})")
        }

        return NetworkResult.Success(todayCheckIns)
    }

    /**
     * Get debug check-outs for March 2027
     */
    private fun getDebugTodayCheckOuts(): NetworkResult<List<CheckOut>> {
        val today = DateUtils.now()
        val testReservations = BalanceTestDataGenerator.generateTestReservations()

        val todayCheckOuts = testReservations
            .filter { it.checkOutDate == today }
            .filter { it.status.name.lowercase() !in listOf("pending", "declined", "cancelled", "admin_cancelled") }
            .map { CheckOut(it, LocalTime.of(11, 0)) }

        android.util.Log.d("DEBUG_DATA", "========== DEBUG MODE ACTIVE ==========")
        android.util.Log.d("DEBUG_DATA", "Generated ${todayCheckOuts.size} test check-outs for $today")
        todayCheckOuts.forEach { checkOut ->
            android.util.Log.d("DEBUG_DATA", "  - ${checkOut.reservation.primaryGuest.fullName} (${checkOut.reservation.reservationNumber})")
        }

        return NetworkResult.Success(todayCheckOuts)
    }

    /**
     * Get debug week bookings for March 2027
     */
    private fun getDebugWeekBookings(): NetworkResult<List<WeekBooking>> {
        val (weekStart, weekEnd) = DateUtils.getCurrentWeekDates()
        val today = DateUtils.now()
        val testReservations = BalanceTestDataGenerator.generateTestReservations()

        val weekBookings = testReservations
            .filter { reservation ->
                val checkInDate = reservation.checkInDate
                !checkInDate.isBefore(weekStart) &&
                        !checkInDate.isAfter(weekEnd) &&
                        !checkInDate.isBefore(today)
            }
            .filter { it.status.name.lowercase() !in listOf("pending", "declined", "cancelled", "admin_cancelled") }
            .map { WeekBooking(it, it.checkInDate.dayOfWeek, it.checkInDate) }
            .sortedBy { it.date }

        android.util.Log.d("DEBUG_DATA", "========== DEBUG MODE ACTIVE ==========")
        android.util.Log.d("DEBUG_DATA", "Generated ${weekBookings.size} test week bookings")

        return NetworkResult.Success(weekBookings)
    }
}