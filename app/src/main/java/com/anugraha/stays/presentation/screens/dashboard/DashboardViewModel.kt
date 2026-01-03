package com.anugraha.stays.presentation.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anugraha.stays.domain.usecase.dashboard.*
import com.anugraha.stays.domain.usecase.reservation.AcceptReservationUseCase
import com.anugraha.stays.domain.usecase.reservation.DeclineReservationUseCase
import com.anugraha.stays.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getTodayCheckInsUseCase: GetTodayCheckInsUseCase,
    private val getTodayCheckOutsUseCase: GetTodayCheckOutsUseCase,
    private val getWeekBookingsUseCase: GetWeekBookingsUseCase,
    private val getPendingReservationsUseCase: GetPendingReservationsUseCase,
    private val acceptReservationUseCase: AcceptReservationUseCase,
    private val declineReservationUseCase: DeclineReservationUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        handleIntent(DashboardIntent.LoadData)
    }

    fun handleIntent(intent: DashboardIntent) {
        when (intent) {
            DashboardIntent.LoadData -> loadAllData()
            DashboardIntent.RefreshData -> refreshData()
            is DashboardIntent.AcceptReservation -> acceptReservation(intent.reservationId)
            is DashboardIntent.DeclineReservation -> declineReservation(intent.reservationId)
        }
    }

    private fun loadAllData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Load all data in parallel
            launch { loadTodayCheckIns() }
            launch { loadTodayCheckOuts() }
            launch { loadWeekBookings() }
            launch { loadPendingReservations() }
        }
    }

    private fun refreshData() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }

            launch { loadTodayCheckIns() }
            launch { loadTodayCheckOuts() }
            launch { loadWeekBookings() }
            launch { loadPendingReservations() }

            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private suspend fun loadTodayCheckIns() {
        when (val result = getTodayCheckInsUseCase()) {
            is NetworkResult.Success -> {
                _state.update {
                    it.copy(
                        todayCheckIns = result.data,
                        isLoading = false
                    )
                }
            }
            is NetworkResult.Error -> {
                _state.update {
                    it.copy(
                        error = result.message,
                        isLoading = false
                    )
                }
            }
            NetworkResult.Loading -> {}
        }
    }

    private suspend fun loadTodayCheckOuts() {
        when (val result = getTodayCheckOutsUseCase()) {
            is NetworkResult.Success -> {
                _state.update {
                    it.copy(todayCheckOuts = result.data)
                }
            }
            is NetworkResult.Error -> {}
            NetworkResult.Loading -> {}
        }
    }

    private suspend fun loadWeekBookings() {
        when (val result = getWeekBookingsUseCase()) {
            is NetworkResult.Success -> {
                _state.update {
                    it.copy(weekBookings = result.data)
                }
            }
            is NetworkResult.Error -> {}
            NetworkResult.Loading -> {}
        }
    }

    private suspend fun loadPendingReservations() {
        when (val result = getPendingReservationsUseCase()) {
            is NetworkResult.Success -> {
                _state.update {
                    it.copy(pendingReservations = result.data)
                }
            }
            is NetworkResult.Error -> {}
            NetworkResult.Loading -> {}
        }
    }

    private fun acceptReservation(reservationId: Int) {
        viewModelScope.launch {
            when (acceptReservationUseCase(reservationId)) {
                is NetworkResult.Success -> {
                    loadPendingReservations()
                }
                is NetworkResult.Error -> {}
                NetworkResult.Loading -> {}
            }
        }
    }

    private fun declineReservation(reservationId: Int) {
        viewModelScope.launch {
            when (declineReservationUseCase(reservationId)) {
                is NetworkResult.Success -> {
                    loadPendingReservations()
                }
                is NetworkResult.Error -> {}
                NetworkResult.Loading -> {}
            }
        }
    }
}