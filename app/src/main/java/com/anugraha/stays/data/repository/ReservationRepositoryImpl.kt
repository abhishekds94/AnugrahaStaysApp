package com.anugraha.stays.data.repository

import android.util.Log
import com.anugraha.stays.data.remote.api.AnugrahaApi
import com.anugraha.stays.data.remote.dto.toDomain
import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.domain.model.ReservationStatus
import com.anugraha.stays.domain.repository.ICalSyncRepository  // ADD THIS
import com.anugraha.stays.domain.repository.ReservationRepository
import com.anugraha.stays.util.NetworkResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import javax.inject.Inject

class ReservationRepositoryImpl @Inject constructor(
    private val api: AnugrahaApi,
    private val iCalSyncRepository: ICalSyncRepository  // ADD THIS
) : ReservationRepository {

    private var cachedReservations: List<Reservation> = emptyList()

    override suspend fun getReservations(
        page: Int,
        perPage: Int,
        status: ReservationStatus?,
        checkInDate: LocalDate?
    ): NetworkResult<List<Reservation>> {
        return try {
            Log.d("ReservationRepo", "Fetching reservations with status: ${status?.name}")

            val response = api.getReservations(
                page = page,
                perPage = perPage,
                status = status?.name?.lowercase(),
                checkInDate = checkInDate?.toString()
            )

            if (response.isSuccessful) {
                val body = response.body()

                if (body.isNullOrEmpty()) {
                    Log.w("ReservationRepo", "API returned empty list")
                    cachedReservations = emptyList()
                    return NetworkResult.Success(emptyList())
                }

                // Fetch full details for API reservations
                val apiReservations = fetchFullReservationDetails(body.map { it.id ?: 0 })

                // GET EXTERNAL BOOKINGS (only if status is null or approved)
                val externalBookings = if (status == null || status == ReservationStatus.APPROVED) {
                    iCalSyncRepository.getExternalBookings()
                } else {
                    emptyList()
                }

                // MERGE
                val allReservations = apiReservations + externalBookings

                Log.d("ReservationRepo", "Total reservations: ${allReservations.size} (${apiReservations.size} API + ${externalBookings.size} external)")
                cachedReservations = allReservations
                NetworkResult.Success(allReservations)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("ReservationRepo", "Error ${response.code()}: $errorBody")
                NetworkResult.Error("Failed to fetch reservations: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("ReservationRepo", "Exception fetching reservations", e)
            NetworkResult.Error(e.message ?: "An error occurred")
        }
    }

    private suspend fun fetchFullReservationDetails(reservationIds: List<Int>): List<Reservation> {
        return coroutineScope {
            reservationIds.map { id ->
                async {
                    try {
                        val response = api.getReservationById(id)
                        if (response.isSuccessful) {
                            response.body()?.toDomain()
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
            }.awaitAll().filterNotNull().sortedByDescending { it.checkInDate }
        }
    }

    override suspend fun getReservationById(id: Int): NetworkResult<Reservation> {
        return try {
            Log.d("ReservationRepo", "Fetching reservation by id: $id")
            val response = api.getReservationById(id)

            if (response.isSuccessful) {
                val reservation = response.body()?.toDomain()
                if (reservation != null) {
                    NetworkResult.Success(reservation)
                } else {
                    NetworkResult.Error("Reservation not found or invalid data")
                }
            } else {
                NetworkResult.Error("Failed to fetch reservation: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "An error occurred")
        }
    }

    override suspend fun acceptReservation(id: Int): NetworkResult<Unit> {
        return try {
            val response = api.acceptReservation(id)
            if (response.isSuccessful) {
                NetworkResult.Success(Unit)
            } else {
                NetworkResult.Error("Failed to accept reservation: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "An error occurred")
        }
    }

    override suspend fun declineReservation(id: Int): NetworkResult<Unit> {
        return try {
            val response = api.declineReservation(id)
            if (response.isSuccessful) {
                NetworkResult.Success(Unit)
            } else {
                NetworkResult.Error("Failed to decline reservation: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "An error occurred")
        }
    }

    override suspend fun searchReservations(query: String): Flow<List<Reservation>> = flow {
        val filtered = cachedReservations.filter { reservation ->
            reservation.primaryGuest.fullName.contains(query, ignoreCase = true) ||
                    reservation.primaryGuest.phone.contains(query) ||
                    reservation.checkInDate.toString().contains(query) ||
                    reservation.checkOutDate.toString().contains(query) ||
                    reservation.reservationNumber.contains(query, ignoreCase = true)
        }
        emit(filtered)
    }
}