package com.anugraha.stays.domain.usecase.availability

import com.anugraha.stays.domain.model.Availability
import com.anugraha.stays.domain.repository.AvailabilityRepository
import com.anugraha.stays.util.NetworkResult
import java.time.YearMonth
import javax.inject.Inject

class GetAvailabilityUseCase @Inject constructor(
    private val availabilityRepository: AvailabilityRepository
) {
    suspend operator fun invoke(yearMonth: YearMonth): NetworkResult<List<Availability>> {
        return availabilityRepository.getAvailabilityForMonth(yearMonth)
    }
}