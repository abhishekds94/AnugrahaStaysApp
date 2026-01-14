package com.anugraha.stays.domain.repository

import com.anugraha.stays.domain.model.ICalConfig
import com.anugraha.stays.domain.model.ICalSource
import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.util.NetworkResult

data class SourceSyncStatus(
    val source: ICalSource,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

interface ICalSyncRepository {
    suspend fun syncICalFeeds(configs: List<ICalConfig>): NetworkResult<List<Reservation>>
    suspend fun syncICalFeedsDetailed(configs: List<ICalConfig>): Pair<NetworkResult<List<Reservation>>, List<SourceSyncStatus>>
    suspend fun getExternalBookings(): List<Reservation>
}