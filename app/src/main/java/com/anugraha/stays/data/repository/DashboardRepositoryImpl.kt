package com.anugraha.stays.data.repository

import com.anugraha.stays.data.remote.api.AnugrahaApi
import com.anugraha.stays.data.remote.dto.ReservationDto
import com.anugraha.stays.data.remote.dto.toDomain
import com.anugraha.stays.domain.model.*
import com.anugraha.stays.domain.repository.DashboardRepository
import com.anugraha.stays.domain.repository.ICalSyncRepository
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
            val response = api.getTodayCheckIns()
            if (!response.isSuccessful) return@coroutineScope NetworkResult.Error("API_ERROR_${response.code()}")

            val reservationDtos = response.body()?.data ?: emptyList()

            val apiCheckIns = reservationDtos.map { dto ->
                async { fetchFullReservationAndMap(dto) }
            }.awaitAll().mapNotNull { reservation ->
                reservation?.let { CheckIn(it, it.checkInDate.atStartOfDay().toLocalTime()) }
            }

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
            val today = DateUtils.now()
            val response = api.getReservations(
                status = "approved",
                perPage = 100,
                sortBy = "check_out_date",
                sortOrder = "asc"
            )

            if (!response.isSuccessful) return@coroutineScope NetworkResult.Error("API_ERROR_${response.code()}")

            val reservationDtos = response.body() ?: emptyList()
            val apiCheckOuts = reservationDtos
                .map { async { fetchFullReservationAndMap(it) } }
                .awaitAll()
                .filter { it?.checkOutDate?.isEqual(today) == true }
                .map { CheckOut(it!!, LocalTime.of(11, 0)) }

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
            val (weekStart, weekEnd) = DateUtils.getCurrentWeekDates()
            val today = DateUtils.now()

            val response = api.getReservations(
                status = "approved",
                perPage = 100,
                sortBy = "check_in_date",
                sortOrder = "asc"
            )

            if (!response.isSuccessful) return@coroutineScope NetworkResult.Error("API_ERROR_${response.code()}")

            val apiWeekBookings = (response.body() ?: emptyList())
                .map { async { fetchFullReservationAndMap(it) } }
                .awaitAll()
                .filterNotNull()
                .filter { res ->
                    val date = res.checkInDate
                    !date.isBefore(weekStart) && !date.isAfter(weekEnd) && !date.isBefore(today)
                }
                .map { WeekBooking(it, it.checkInDate.dayOfWeek, it.checkInDate) }

            val externalWeekBookings = iCalSyncRepository.getExternalBookings()
                .filter { res ->
                    val date = res.checkInDate
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
}