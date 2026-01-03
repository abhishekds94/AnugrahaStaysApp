package com.anugraha.stays.domain.repository

import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.domain.model.ReservationStatus
import com.anugraha.stays.util.NetworkResult
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface ReservationRepository {
    suspend fun getReservations(
        page: Int = 1,
        perPage: Int = 10,
        status: ReservationStatus? = null,
        checkInDate: LocalDate? = null
    ): NetworkResult<List<Reservation>>

    suspend fun getReservationById(id: Int): NetworkResult<Reservation>

    suspend fun acceptReservation(id: Int): NetworkResult<Unit>

    suspend fun declineReservation(id: Int): NetworkResult<Unit>

    suspend fun searchReservations(query: String): Flow<List<Reservation>>
}