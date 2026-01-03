package com.anugraha.stays.domain.usecase.dashboard

import com.anugraha.stays.domain.model.WeekBooking
import com.anugraha.stays.domain.repository.DashboardRepository
import com.anugraha.stays.util.NetworkResult
import javax.inject.Inject

class GetWeekBookingsUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository
) {
    suspend operator fun invoke(): NetworkResult<List<WeekBooking>> {
        return dashboardRepository.getThisWeekBookings()
    }
}