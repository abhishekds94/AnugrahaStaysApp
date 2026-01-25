package com.anugraha.stays.presentation.screens.dashboard

import androidx.lifecycle.viewModelScope
import com.anugraha.stays.domain.usecase.dashboard.GetPendingReservationsUseCase
import com.anugraha.stays.domain.usecase.dashboard.GetTodayCheckInsUseCase
import com.anugraha.stays.domain.usecase.dashboard.GetTodayCheckOutsUseCase
import com.anugraha.stays.domain.usecase.dashboard.GetWeekBookingsUseCase
import com.anugraha.stays.domain.usecase.ical.SyncICalFeedsUseCase
import com.anugraha.stays.domain.usecase.reservation.AcceptReservationUseCase
import com.anugraha.stays.domain.usecase.reservation.DeclineReservationUseCase
import com.anugraha.stays.util.BaseViewModel
import com.anugraha.stays.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getTodayCheckInsUseCase: GetTodayCheckInsUseCase,
    private val getTodayCheckOutsUseCase: GetTodayCheckOutsUseCase,
    private val getWeekBookingsUseCase: GetWeekBookingsUseCase,
    private val getPendingReservationsUseCase: GetPendingReservationsUseCase,
    private val acceptReservationUseCase: AcceptReservationUseCase,
    private val declineReservationUseCase: DeclineReservationUseCase,
    private val syncICalFeedsUseCase: SyncICalFeedsUseCase
) : BaseViewModel<DashboardState, DashboardIntent, DashboardEffect>(DashboardState()) {

    init {
        handleIntent(DashboardIntent.LoadData)
    }

    override fun handleIntent(intent: DashboardIntent) {
        when (intent) {
            is DashboardIntent.LoadData -> loadData()
            is DashboardIntent.RefreshData -> refreshData()
            is DashboardIntent.SyncExternalBookings -> syncExternalBookings()
            is DashboardIntent.AcceptReservation -> acceptReservation(intent.id)
            is DashboardIntent.DeclineReservation -> declineReservation(intent.id)
            is DashboardIntent.ForceResync -> forceResync()
        }
    }

    private fun syncExternalBookings() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }

            when (val result = syncICalFeedsUseCase()) {
                is NetworkResult.Success -> {
                    loadData()
                }
                is NetworkResult.Error -> {
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                    sendEffect(DashboardEffect.ShowError(result.message ?: "Sync failed"))
                }
                NetworkResult.Loading -> {}
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            updateState {
                it.copy(
                    isLoading = true,
                    isLoadingCheckIns = true,
                    isLoadingCheckOuts = true,
                    isLoadingWeekBookings = true,
                    isLoadingPendingReservations = true
                )
            }

            listOf(
                async { loadTodayCheckIns() },
                async { loadTodayCheckOuts() },
                async { loadWeekBookings() },
                async { loadPendingReservations() }
            ).awaitAll()

            updateState { it.copy(isLoading = false) }
        }
    }

    private fun refreshData() {
        viewModelScope.launch {
            updateState { it.copy(isRefreshing = true, error = null) }

            listOf(
                async { loadTodayCheckIns() },
                async { loadTodayCheckOuts() },
                async { loadWeekBookings() },
                async { loadPendingReservations() }
            ).awaitAll()

            updateState { it.copy(isRefreshing = false) }
        }
    }

    private suspend fun loadTodayCheckIns() {
        when (val result = getTodayCheckInsUseCase()) {
            is NetworkResult.Success -> {
                updateState {
                    it.copy(
                        todayCheckIns = result.data ?: emptyList(),
                        isLoadingCheckIns = false
                    )
                }
            }
            is NetworkResult.Error -> {
                updateState {
                    it.copy(
                        error = result.message,
                        isLoadingCheckIns = false
                    )
                }
            }
            NetworkResult.Loading -> {}
        }
    }

    private suspend fun loadTodayCheckOuts() {
        when (val result = getTodayCheckOutsUseCase()) {
            is NetworkResult.Success -> {
                updateState {
                    it.copy(
                        todayCheckOuts = result.data ?: emptyList(),
                        isLoadingCheckOuts = false
                    )
                }
            }
            is NetworkResult.Error -> {
                updateState {
                    it.copy(
                        error = result.message,
                        isLoadingCheckOuts = false
                    )
                }
            }
            NetworkResult.Loading -> {}
        }
    }

    private suspend fun loadWeekBookings() {
        when (val result = getWeekBookingsUseCase()) {
            is NetworkResult.Success -> {
                updateState {
                    it.copy(
                        weekBookings = result.data ?: emptyList(),
                        isLoadingWeekBookings = false
                    )
                }
            }
            is NetworkResult.Error -> {
                updateState {
                    it.copy(
                        error = result.message,
                        isLoadingWeekBookings = false
                    )
                }
            }
            NetworkResult.Loading -> {}
        }
    }

    private suspend fun loadPendingReservations() {
        when (val result = getPendingReservationsUseCase()) {
            is NetworkResult.Success -> {
                updateState {
                    it.copy(
                        pendingReservations = result.data ?: emptyList(),
                        isLoadingPendingReservations = false
                    )
                }
            }
            is NetworkResult.Error -> {
                updateState {
                    it.copy(
                        error = result.message,
                        isLoadingPendingReservations = false
                    )
                }
            }
            NetworkResult.Loading -> {}
        }
    }

    private fun acceptReservation(id: Int) {
        viewModelScope.launch {
            when (val result = acceptReservationUseCase(id)) {
                is NetworkResult.Success -> {
                    sendEffect(DashboardEffect.ShowToast("Reservation accepted"))
                    refreshData()
                }
                is NetworkResult.Error -> {
                    sendEffect(DashboardEffect.ShowError(result.message ?: "Failed to accept"))
                }
                NetworkResult.Loading -> {}
            }
        }
    }

    private fun declineReservation(id: Int) {
        viewModelScope.launch {
            when (val result = declineReservationUseCase(id)) {
                is NetworkResult.Success -> {
                    sendEffect(DashboardEffect.ShowToast("Reservation declined"))
                    refreshData()
                }
                is NetworkResult.Error -> {
                    sendEffect(DashboardEffect.ShowError(result.message ?: "Failed to decline"))
                }
                NetworkResult.Loading -> {}
            }
        }
    }

    private fun forceResync() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            when (val result = syncICalFeedsUseCase()) {
                is NetworkResult.Success -> {
                    loadData()
                    sendEffect(DashboardEffect.ShowToast("Bookings data updated!"))
                }
                is NetworkResult.Error -> {
                    updateState { it.copy(isLoading = false) }
                    sendEffect(DashboardEffect.ShowError(result.message ?: "Resync failed"))
                }
                NetworkResult.Loading -> {}
            }
        }
    }
}
