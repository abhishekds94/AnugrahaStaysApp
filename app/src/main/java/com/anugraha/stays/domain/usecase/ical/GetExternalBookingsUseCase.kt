package com.anugraha.stays.domain.usecase.ical

import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.domain.repository.ICalSyncRepository
import com.anugraha.stays.util.AdvancedBookingDeduplicator
import javax.inject.Inject

class GetExternalBookingsUseCase @Inject constructor(
    private val repository: ICalSyncRepository,
    private val advancedDeduplicator: AdvancedBookingDeduplicator
) {
    suspend operator fun invoke(): List<Reservation> {
        val bookings = repository.getExternalBookings()
        // Note: Main deduplication happens in ReservationsViewModel
        // This is just for when external bookings are fetched independently
        return bookings
    }
}