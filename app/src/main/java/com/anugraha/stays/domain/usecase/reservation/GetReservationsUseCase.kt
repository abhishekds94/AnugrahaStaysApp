package com.anugraha.stays.domain.usecase.reservation

import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.domain.model.ReservationStatus
import com.anugraha.stays.domain.repository.ReservationRepository
import com.anugraha.stays.util.NetworkResult
import java.time.LocalDate
import javax.inject.Inject

class GetReservationsUseCase @Inject constructor(
    private val reservationRepository: ReservationRepository
) {
    suspend operator fun invoke(
        page: Int = 1,
        perPage: Int = 10,
        status: ReservationStatus? = ReservationStatus.APPROVED
    ): NetworkResult<List<Reservation>> {
        return reservationRepository.getReservations(
            page = page,
            perPage = perPage,
            status = status
        )
    }
}