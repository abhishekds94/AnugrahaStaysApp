package com.anugraha.stays.domain.usecase.dashboard

import com.anugraha.stays.domain.model.CheckOut
import com.anugraha.stays.domain.repository.DashboardRepository
import com.anugraha.stays.util.NetworkResult
import javax.inject.Inject

class GetTodayCheckOutsUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository
) {
    suspend operator fun invoke(): NetworkResult<List<CheckOut>> {
        return dashboardRepository.getTodayCheckOuts()
    }
}