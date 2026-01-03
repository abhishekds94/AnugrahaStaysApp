package com.anugraha.stays.domain.usecase.dashboard

import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.domain.repository.DashboardRepository
import com.anugraha.stays.util.NetworkResult
import javax.inject.Inject

class GetPendingReservationsUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository
) {
    suspend operator fun invoke(): NetworkResult<List<Reservation>> {
        return dashboardRepository.getPendingReservations()
    }
}