package com.anugraha.stays.domain.usecase.ical

import com.anugraha.stays.domain.model.ICalConfig
import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.domain.repository.ICalSyncRepository
import com.anugraha.stays.util.NetworkResult
import javax.inject.Inject

class SyncICalFeedsUseCase @Inject constructor(
    private val repository: ICalSyncRepository
) {
    suspend operator fun invoke(): NetworkResult<List<Reservation>> {
        val configs = ICalConfig.getDefaultConfigs()
        return repository.syncICalFeeds(configs)
    }
}