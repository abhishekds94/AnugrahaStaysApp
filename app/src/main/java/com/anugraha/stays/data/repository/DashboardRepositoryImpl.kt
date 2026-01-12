package com.anugraha.stays.data.repository

import android.util.Log
import com.anugraha.stays.data.remote.api.AnugrahaApi
import com.anugraha.stays.data.remote.dto.toDomain
import com.anugraha.stays.domain.model.*
import com.anugraha.stays.domain.repository.DashboardRepository
import com.anugraha.stays.domain.repository.ICalSyncRepository
import com.anugraha.stays.util.DateUtils
import com.anugraha.stays.util.NetworkResult
import java.time.LocalDate
import javax.inject.Inject

class DashboardRepositoryImpl @Inject constructor(
    private val api: AnugrahaApi,
    private val iCalSyncRepository: ICalSyncRepository  // ADD THIS
) : DashboardRepository {

    override suspend fun getTodayCheckIns(): NetworkResult<List<CheckIn>> {
        return try {
            Log.d("DashboardRepo", "Fetching today's check-ins")
            val response = api.getTodayCheckIns()

            if (response.isSuccessful) {
                val body = response.body()
                val reservationDtos = body?.data ?: emptyList()
                Log.d("DashboardRepo", "Found ${reservationDtos.size} check-in DTOs from API")

                // Fetch full details for each API reservation
                val apiCheckIns = reservationDtos.mapNotNull { incompleteDto ->
                    try {
                        if (incompleteDto.id == null) return@mapNotNull null

                        val fullResponse = api.getReservationById(incompleteDto.id)
                        val fullDto = if (fullResponse.isSuccessful) {
                            fullResponse.body()
                        } else {
                            incompleteDto
                        }

                        fullDto?.toDomain()?.let { reservation ->
                            CheckIn(
                                reservation = reservation,
                                checkInTime = fullDto.estimatedCheckInTime?.let {
                                    try {
                                        java.time.LocalTime.parse(it)
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                            )
                        }
                    } catch (e: Exception) {
                        incompleteDto.toDomain()?.let { CheckIn(it, null) }
                    }
                }

                // GET EXTERNAL BOOKINGS
                val externalBookings = iCalSyncRepository.getExternalBookings()
                val today = LocalDate.now()

                val externalCheckIns = externalBookings
                    .filter { it.checkInDate == today }
                    .map { CheckIn(reservation = it, checkInTime = null) }

                // MERGE
                val allCheckIns = apiCheckIns + externalCheckIns

                Log.d("DashboardRepo", "Total check-ins: ${allCheckIns.size} (${apiCheckIns.size} API + ${externalCheckIns.size} external)")
                NetworkResult.Success(allCheckIns)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("DashboardRepo", "API Error ${response.code()}: $errorBody")
                NetworkResult.Error("Failed to fetch check-ins: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("DashboardRepo", "Exception fetching check-ins", e)
            NetworkResult.Error(e.message ?: "An error occurred")
        }
    }

    override suspend fun getTodayCheckOuts(): NetworkResult<List<CheckOut>> {
        return try {
            val today = LocalDate.now()
            Log.d("DashboardRepo", "Computing today's check-outs for date: $today")

            val response = api.getReservations(
                status = "approved",
                perPage = 100,
                sortBy = "check_out_date",
                sortOrder = "asc"
            )

            if (response.isSuccessful) {
                val reservationDtos = response.body() ?: emptyList()

                // Fetch full details and filter for today's API check-outs
                val apiCheckOuts = reservationDtos
                    .mapNotNull { incompleteDto ->
                        try {
                            if (incompleteDto.id == null) return@mapNotNull null
                            val fullResponse = api.getReservationById(incompleteDto.id)
                            val fullDto = if (fullResponse.isSuccessful) {
                                fullResponse.body()
                            } else {
                                incompleteDto
                            }
                            fullDto?.toDomain()
                        } catch (e: Exception) {
                            incompleteDto.toDomain()
                        }
                    }
                    .filter { it.checkOutDate.isEqual(today) }
                    .map { CheckOut(it, java.time.LocalTime.of(11, 0)) }

                // GET EXTERNAL BOOKINGS
                val externalBookings = iCalSyncRepository.getExternalBookings()
                val externalCheckOuts = externalBookings
                    .filter { it.checkOutDate == today }
                    .map { CheckOut(it, java.time.LocalTime.of(11, 0)) }

                // MERGE
                val allCheckOuts = apiCheckOuts + externalCheckOuts

                Log.d("DashboardRepo", "Total check-outs: ${allCheckOuts.size}")
                NetworkResult.Success(allCheckOuts)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("DashboardRepo", "Error ${response.code()}: $errorBody")
                NetworkResult.Error("Failed to fetch reservations: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("DashboardRepo", "Exception computing check-outs", e)
            NetworkResult.Error(e.message ?: "An error occurred")
        }
    }

    override suspend fun getThisWeekBookings(): NetworkResult<List<WeekBooking>> {
        return try {
            val (weekStart, weekEnd) = DateUtils.getCurrentWeekDates()
            val today = LocalDate.now()

            val response = api.getReservations(
                status = "approved",
                perPage = 100,
                sortBy = "check_in_date",
                sortOrder = "asc"
            )

            if (response.isSuccessful) {
                val reservationDtos = response.body() ?: emptyList()

                // Fetch full details and filter API bookings
                val apiWeekBookings = reservationDtos
                    .mapNotNull { incompleteDto ->
                        try {
                            if (incompleteDto.id == null) return@mapNotNull null
                            val fullResponse = api.getReservationById(incompleteDto.id)
                            val fullDto = if (fullResponse.isSuccessful) {
                                fullResponse.body()
                            } else {
                                incompleteDto
                            }
                            fullDto?.toDomain()
                        } catch (e: Exception) {
                            incompleteDto.toDomain()
                        }
                    }
                    .filter { reservation ->
                        val checkInDate = reservation.checkInDate
                        !checkInDate.isBefore(weekStart) &&
                                !checkInDate.isAfter(weekEnd) &&
                                !checkInDate.isBefore(today)
                    }
                    .map { WeekBooking(it, it.checkInDate.dayOfWeek, it.checkInDate) }

                // GET EXTERNAL BOOKINGS
                val externalBookings = iCalSyncRepository.getExternalBookings()
                val externalWeekBookings = externalBookings
                    .filter { reservation ->
                        val checkInDate = reservation.checkInDate
                        !checkInDate.isBefore(weekStart) &&
                                !checkInDate.isAfter(weekEnd) &&
                                !checkInDate.isBefore(today)
                    }
                    .map { WeekBooking(it, it.checkInDate.dayOfWeek, it.checkInDate) }

                // MERGE AND SORT
                val allBookings = (apiWeekBookings + externalWeekBookings)
                    .sortedBy { it.date }

                // ADD THIS LOGGING
                Log.d("DashboardRepo", "")
                Log.d("DashboardRepo", "📅 THIS WEEK BOOKINGS MERGE:")
                Log.d("DashboardRepo", "   API bookings: ${apiWeekBookings.size}")
                Log.d("DashboardRepo", "   External bookings: ${externalWeekBookings.size}")
                Log.d("DashboardRepo", "   Total: ${allBookings.size}")
                Log.d("DashboardRepo", "")

                externalWeekBookings.forEach { booking ->
                    Log.d("DashboardRepo", "   🌐 External: ${booking.reservation.bookingSource.displayName()}")
                    Log.d("DashboardRepo", "      Date: ${booking.date}")
                    Log.d("DashboardRepo", "      Guest: ${booking.reservation.primaryGuest.fullName}")
                }

                Log.d("DashboardRepo", "Total week bookings: ${allBookings.size}")

                Log.d("DashboardRepo", "Total week bookings: ${allBookings.size}")
                NetworkResult.Success(allBookings)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("DashboardRepo", "Error ${response.code()}: $errorBody")
                NetworkResult.Error("Failed to fetch week bookings: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("DashboardRepo", "Exception computing week bookings", e)
            NetworkResult.Error(e.message ?: "An error occurred")
        }
    }

    override suspend fun getPendingReservations(): NetworkResult<List<Reservation>> {
        return try {
            Log.d("DashboardRepo", "Fetching pending reservations")
            val response = api.getReservations(
                status = "pending",
                perPage = 50,
                sortBy = "check_in_date",
                sortOrder = "asc"
            )

            if (response.isSuccessful) {
                val reservationDtos = response.body() ?: emptyList()

                val reservations = reservationDtos.mapNotNull { incompleteDto ->
                    try {
                        if (incompleteDto.id == null) return@mapNotNull null
                        val fullResponse = api.getReservationById(incompleteDto.id)
                        val fullDto = if (fullResponse.isSuccessful) {
                            fullResponse.body()
                        } else {
                            incompleteDto
                        }
                        fullDto?.toDomain()
                    } catch (e: Exception) {
                        incompleteDto.toDomain()
                    }
                }

                Log.d("DashboardRepo", "Got ${reservations.size} pending reservations")
                NetworkResult.Success(reservations)
            } else {
                NetworkResult.Error("Failed to fetch pending reservations: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("DashboardRepo", "Exception: ${e.message}", e)
            NetworkResult.Error(e.message ?: "An error occurred")
        }
    }
}