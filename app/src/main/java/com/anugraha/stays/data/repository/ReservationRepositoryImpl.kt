package com.anugraha.stays.data.repository

import android.util.Log
import com.anugraha.stays.data.remote.api.AnugrahaApi
import com.anugraha.stays.data.remote.dto.toDomain
import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.domain.model.ReservationStatus
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
    private val api: AnugrahaApi
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

            Log.d("ReservationRepo", "Response code: ${response.code()}")

            if (response.isSuccessful) {
                val body = response.body()
                Log.d("ReservationRepo", "Raw response body size: ${body?.size}")

                if (body.isNullOrEmpty()) {
                    Log.w("ReservationRepo", "API returned empty list")
                    cachedReservations = emptyList()
                    return NetworkResult.Success(emptyList())
                }

                // SOLUTION: Fetch full details for each reservation to get guest names
                val reservations = fetchFullReservationDetails(body.map { it.id ?: 0 })

                Log.d("ReservationRepo", "Successfully fetched ${reservations.size} reservations with full details")
                cachedReservations = reservations
                NetworkResult.Success(reservations)
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

    /**
     * Fetches full reservation details (including guest info) for each reservation ID
     * This solves the issue where the list endpoint doesn't include nested objects
     */
    private suspend fun fetchFullReservationDetails(reservationIds: List<Int>): List<Reservation> {
        return coroutineScope {
            reservationIds.map { id ->
                async {
                    try {
                        Log.d("ReservationRepo", "Fetching full details for reservation ID: $id")
                        val response = api.getReservationById(id)
                        if (response.isSuccessful) {
                            response.body()?.toDomain()?.also {
                                Log.d("ReservationRepo", "Got full details: ${it.reservationNumber} - ${it.primaryGuest.fullName}")
                            }
                        } else {
                            Log.e("ReservationRepo", "Failed to fetch details for ID $id: ${response.code()}")
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("ReservationRepo", "Exception fetching details for ID $id", e)
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
                    Log.d("ReservationRepo", "Found reservation: ${reservation.reservationNumber} - ${reservation.primaryGuest.fullName}")
                    NetworkResult.Success(reservation)
                } else {
                    Log.e("ReservationRepo", "Failed to parse reservation data")
                    NetworkResult.Error("Reservation not found or invalid data")
                }
            } else {
                Log.e("ReservationRepo", "Error ${response.code()}: ${response.errorBody()?.string()}")
                NetworkResult.Error("Failed to fetch reservation: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("ReservationRepo", "Exception fetching reservation by id", e)
            NetworkResult.Error(e.message ?: "An error occurred")
        }
    }

    override suspend fun acceptReservation(id: Int): NetworkResult<Unit> {
        return try {
            val response = api.acceptReservation(id)
            if (response.isSuccessful) {
                Log.d("ReservationRepo", "Accepted reservation: $id")
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
            android.util.Log.d("ReservationRepo", "========== DECLINE RESERVATION ==========")
            android.util.Log.d("ReservationRepo", "Reservation ID: $id")
            android.util.Log.d("ReservationRepo", "Calling API: POST /reservation/$id/decline")

            val response = api.declineReservation(id)

            android.util.Log.d("ReservationRepo", "Response code: ${response.code()}")
            android.util.Log.d("ReservationRepo", "Response message: ${response.message()}")
            android.util.Log.d("ReservationRepo", "Response body: ${response.body()}")
            android.util.Log.d("ReservationRepo", "Response error body: ${response.errorBody()?.string()}")

            if (response.isSuccessful) {
                android.util.Log.d("ReservationRepo", "✅ Declined reservation: $id")
                android.util.Log.d("ReservationRepo", "========================================")
                NetworkResult.Success(Unit)
            } else {
                android.util.Log.e("ReservationRepo", "❌ Failed to decline reservation: ${response.message()}")
                android.util.Log.d("ReservationRepo", "========================================")
                NetworkResult.Error("Failed to decline reservation: ${response.message()}")
            }
        } catch (e: Exception) {
            android.util.Log.e("ReservationRepo", "❌ Exception in declineReservation: ${e.message}", e)
            android.util.Log.d("ReservationRepo", "========================================")
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