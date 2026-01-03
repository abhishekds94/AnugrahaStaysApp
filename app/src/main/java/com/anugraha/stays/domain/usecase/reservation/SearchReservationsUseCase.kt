package com.anugraha.stays.domain.usecase.reservation

import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.domain.repository.ReservationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchReservationsUseCase @Inject constructor(
    private val reservationRepository: ReservationRepository
) {
    suspend operator fun invoke(query: String): Flow<List<Reservation>> {
        return reservationRepository.searchReservations(query)
    }
}