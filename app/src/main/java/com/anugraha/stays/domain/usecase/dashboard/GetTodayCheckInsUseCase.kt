package com.anugraha.stays.domain.usecase.dashboard

import com.anugraha.stays.domain.model.CheckIn
import com.anugraha.stays.domain.repository.DashboardRepository
import com.anugraha.stays.util.NetworkResult
import javax.inject.Inject

class GetTodayCheckInsUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository
) {
    suspend operator fun invoke(): NetworkResult<List<CheckIn>> {
        return dashboardRepository.getTodayCheckIns()
    }
}