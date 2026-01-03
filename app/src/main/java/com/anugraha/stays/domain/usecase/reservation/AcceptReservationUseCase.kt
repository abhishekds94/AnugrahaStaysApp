package com.anugraha.stays.domain.usecase.reservation

import com.anugraha.stays.domain.repository.ReservationRepository
import com.anugraha.stays.util.NetworkResult
import javax.inject.Inject

class AcceptReservationUseCase @Inject constructor(
    private val reservationRepository: ReservationRepository
) {
    suspend operator fun invoke(reservationId: Int): NetworkResult<Unit> {
        return reservationRepository.acceptReservation(reservationId)
    }
}