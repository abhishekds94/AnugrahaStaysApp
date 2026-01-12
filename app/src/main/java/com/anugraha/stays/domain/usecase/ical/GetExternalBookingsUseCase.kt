package com.anugraha.stays.domain.usecase.ical

import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.domain.repository.ICalSyncRepository
import javax.inject.Inject

class GetExternalBookingsUseCase @Inject constructor(
    private val repository: ICalSyncRepository
) {
    suspend operator fun invoke(): List<Reservation> {
        return repository.getExternalBookings()
    }
}