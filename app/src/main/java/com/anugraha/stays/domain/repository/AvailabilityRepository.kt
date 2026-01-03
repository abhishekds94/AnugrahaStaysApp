package com.anugraha.stays.domain.repository

import com.anugraha.stays.domain.model.Availability
import com.anugraha.stays.domain.model.AvailabilityStatus
import com.anugraha.stays.util.NetworkResult
import java.time.LocalDate
import java.time.YearMonth

interface AvailabilityRepository {
    suspend fun getAvailabilityForMonth(yearMonth: YearMonth): NetworkResult<List<Availability>>
    suspend fun getAvailabilityForDate(date: LocalDate): NetworkResult<Availability?>
    suspend fun updateAvailability(
        date: LocalDate,
        status: AvailabilityStatus,
        roomId: Int? = null
    ): NetworkResult<Unit>
}