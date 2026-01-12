package com.anugraha.stays.domain.repository

import com.anugraha.stays.domain.model.ICalConfig
import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.util.NetworkResult

interface ICalSyncRepository {
    suspend fun syncICalFeeds(configs: List<ICalConfig>): NetworkResult<List<Reservation>>
    suspend fun getExternalBookings(): List<Reservation>
}