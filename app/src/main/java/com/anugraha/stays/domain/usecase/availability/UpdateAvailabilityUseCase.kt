package com.anugraha.stays.domain.usecase.availability

import com.anugraha.stays.domain.model.AvailabilityStatus
import com.anugraha.stays.domain.repository.AvailabilityRepository
import com.anugraha.stays.util.NetworkResult
import java.time.LocalDate
import javax.inject.Inject

class UpdateAvailabilityUseCase @Inject constructor(
    private val availabilityRepository: AvailabilityRepository
) {
    suspend operator fun invoke(
        date: LocalDate,
        status: AvailabilityStatus,
        roomId: Int? = 1  // Add roomId parameter with default null
    ): NetworkResult<Unit> {
        return availabilityRepository.updateAvailability(date, status, roomId)
    }
}