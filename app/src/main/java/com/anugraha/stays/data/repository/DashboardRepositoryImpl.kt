package com.anugraha.stays.data.repository

import android.util.Log
import com.anugraha.stays.data.remote.api.AnugrahaApi
import com.anugraha.stays.data.remote.dto.toDomain
import com.anugraha.stays.domain.model.*
import com.anugraha.stays.domain.repository.DashboardRepository
import com.anugraha.stays.util.DateUtils
import com.anugraha.stays.util.NetworkResult
import java.time.LocalDate
import javax.inject.Inject

class DashboardRepositoryImpl @Inject constructor(
    private val api: AnugrahaApi
) : DashboardRepository {

    override suspend fun getTodayCheckIns(): NetworkResult<List<CheckIn>> {
        return try {
            Log.d("DashboardRepo", "Fetching today's check-ins from /today-checkins")
            val response = api.getTodayCheckIns()

            Log.d("DashboardRepo", "Response code: ${response.code()}")

            if (response.isSuccessful) {
                val body = response.body()
                Log.d("DashboardRepo", "Response body: $body")

                val reservationDtos = body?.data ?: emptyList()
                Log.d("DashboardRepo", "Found ${reservationDtos.size} check-in DTOs")

                // Fetch full details for each reservation
                val checkIns = reservationDtos.mapNotNull { incompleteDto ->
                    try {
                        if (incompleteDto.id == null) {
                            Log.e("DashboardRepo", "Reservation ID is null, skipping")
                            return@mapNotNull null
                        }

                        Log.d("DashboardRepo", "Fetching full details for reservation ${incompleteDto.id}")

                        // Fetch full reservation data
                        val fullResponse = api.getReservationById(incompleteDto.id)

                        val fullDto = if (fullResponse.isSuccessful) {
                            fullResponse.body()?.also {
                                Log.d("DashboardRepo", "✅ Got full data for ${incompleteDto.id}: Guest = ${it.primaryGuest?.fullName}")
                            }
                        } else {
                            Log.w("DashboardRepo", "Failed to fetch full details, using incomplete data")
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
                        Log.e("DashboardRepo", "Error fetching full reservation: ${e.message}")
                        // Fallback to incomplete data
                        incompleteDto.toDomain()?.let { reservation ->
                            CheckIn(
                                reservation = reservation,
                                checkInTime = incompleteDto.estimatedCheckInTime?.let {
                                    try {
                                        java.time.LocalTime.parse(it)
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                            )
                        }
                    }
                }

                Log.d("DashboardRepo", "Successfully parsed ${checkIns.size} check-ins")
                NetworkResult.Success(checkIns)
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

            // Fetch all approved reservations
            val response = api.getReservations(
                status = "approved",
                perPage = 100,
                sortBy = "check_out_date",
                sortOrder = "asc"
            )

            if (response.isSuccessful) {
                val reservationDtos = response.body() ?: emptyList()

                Log.d("DashboardRepo", "Fetched ${reservationDtos.size} total approved reservations")

                // Fetch full details and filter for today's check-outs
                val todayCheckOuts = reservationDtos
                    .mapNotNull { incompleteDto ->
                        try {
                            if (incompleteDto.id == null) return@mapNotNull null

                            // Fetch full reservation data
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
                        val checkOutDate = reservation.checkOutDate
                        val isToday = checkOutDate.isEqual(today)

                        if (isToday) {
                            Log.d("DashboardRepo", "Found check-out: ${reservation.reservationNumber} - ${reservation.primaryGuest.fullName} checking out today")
                        }

                        isToday
                    }
                    .map { reservation ->
                        CheckOut(
                            reservation = reservation,
                            checkOutTime = java.time.LocalTime.of(11, 0)
                        )
                    }

                Log.d("DashboardRepo", "Computed ${todayCheckOuts.size} check-outs for today")

                if (todayCheckOuts.isEmpty()) {
                    Log.i("DashboardRepo", "No check-outs scheduled for today ($today)")
                } else {
                    todayCheckOuts.forEach { checkOut ->
                        Log.d("DashboardRepo", "  - ${checkOut.reservation.reservationNumber}: ${checkOut.reservation.primaryGuest.fullName}")
                    }
                }

                NetworkResult.Success(todayCheckOuts)
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
            Log.d("DashboardRepo", "Computing this week's bookings (Sunday to Saturday)")

            val (weekStart, weekEnd) = DateUtils.getCurrentWeekDates()
            val today = LocalDate.now()

            Log.d("DashboardRepo", "Week range: $weekStart to $weekEnd")
            Log.d("DashboardRepo", "Today: $today")

            val response = api.getReservations(
                status = "approved",
                perPage = 100,
                sortBy = "check_in_date",
                sortOrder = "asc"
            )

            if (response.isSuccessful) {
                val reservationDtos = response.body() ?: emptyList()

                Log.d("DashboardRepo", "Fetched ${reservationDtos.size} total approved reservations")

                // Fetch full details for each reservation
                val weekBookings = reservationDtos
                    .mapNotNull { incompleteDto ->
                        try {
                            if (incompleteDto.id == null) {
                                Log.e("DashboardRepo", "Reservation ID is null, skipping")
                                return@mapNotNull null
                            }

                            Log.d("DashboardRepo", "Fetching full details for upcoming booking ${incompleteDto.id}")

                            // Fetch full reservation data from /api/reservations/{id}
                            val fullResponse = api.getReservationById(incompleteDto.id)

                            val fullDto = if (fullResponse.isSuccessful) {
                                fullResponse.body()?.also {
                                    Log.d("DashboardRepo", "✅ Got full data: Guest = ${it.primaryGuest?.fullName}")
                                }
                            } else {
                                Log.w("DashboardRepo", "Failed to fetch full details for ${incompleteDto.id}, using incomplete data")
                                incompleteDto
                            }

                            fullDto?.toDomain()
                        } catch (e: Exception) {
                            Log.e("DashboardRepo", "Error fetching full reservation ${incompleteDto.id}: ${e.message}")
                            incompleteDto.toDomain()
                        }
                    }
                    .filter { reservation ->
                        val checkInDate = reservation.checkInDate

                        // Check if within this week AND not in the past
                        val isInWeekRange = !checkInDate.isBefore(weekStart) &&
                                !checkInDate.isAfter(weekEnd)
                        val isNotPast = !checkInDate.isBefore(today)

                        val shouldInclude = isInWeekRange && isNotPast

                        if (shouldInclude) {
                            Log.d("DashboardRepo", "Including: ${reservation.reservationNumber} - ${reservation.primaryGuest.fullName} - Check-in: $checkInDate (${DateUtils.getRelativeDayName(checkInDate)})")
                        }

                        shouldInclude
                    }
                    .map { reservation ->
                        WeekBooking(
                            reservation = reservation,
                            dayOfWeek = reservation.checkInDate.dayOfWeek,
                            date = reservation.checkInDate
                        )
                    }
                    .sortedBy { it.date }

                Log.d("DashboardRepo", "Computed ${weekBookings.size} upcoming bookings this week")

                if (weekBookings.isEmpty()) {
                    Log.i("DashboardRepo", "No upcoming check-ins this week ($weekStart to $weekEnd)")
                }

                NetworkResult.Success(weekBookings)
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

                // Fetch full details for pending reservations too
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