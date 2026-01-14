package com.anugraha.stays.presentation.screens.dashboard

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anugraha.stays.domain.usecase.dashboard.GetPendingReservationsUseCase
import com.anugraha.stays.domain.usecase.dashboard.GetTodayCheckInsUseCase
import com.anugraha.stays.domain.usecase.dashboard.GetTodayCheckOutsUseCase
import com.anugraha.stays.domain.usecase.dashboard.GetWeekBookingsUseCase
import com.anugraha.stays.domain.usecase.ical.SyncICalFeedsUseCase  // ADD THIS
import com.anugraha.stays.domain.usecase.reservation.AcceptReservationUseCase
import com.anugraha.stays.domain.usecase.reservation.DeclineReservationUseCase
import com.anugraha.stays.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    private val declineReservationUseCase: DeclineReservationUseCase,
    private val syncICalFeedsUseCase: SyncICalFeedsUseCase  // ADD THIS
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    init {
        handleIntent(DashboardIntent.LoadData)
    }

    fun handleIntent(intent: DashboardIntent) {
        when (intent) {
            is DashboardIntent.LoadData -> loadData()
            is DashboardIntent.RefreshData -> refreshData()
            is DashboardIntent.SyncExternalBookings -> syncExternalBookings()  // ADD THIS
            is DashboardIntent.AcceptReservation -> acceptReservation(intent.id)
            is DashboardIntent.DeclineReservation -> declineReservation(intent.id)
            is DashboardIntent.ForceResync -> forceResync()
        }
    }

    private fun syncExternalBookings() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            when (val result = syncICalFeedsUseCase()) {
                is NetworkResult.Success -> {
                    Log.d("DashboardVM", "Synced ${result.data.size} external bookings")
                    loadData() // Refresh dashboard to show synced bookings
                }
                is NetworkResult.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                NetworkResult.Loading -> {}
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    isLoadingCheckIns = true,
                    isLoadingCheckOuts = true,
                    isLoadingWeekBookings = true,
                    isLoadingPendingReservations = true
                )
            }

            launch { loadTodayCheckIns() }
            launch { loadTodayCheckOuts() }
            launch { loadWeekBookings() }
            launch { loadPendingReservations() }

            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun refreshData() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }

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
                        isLoadingCheckIns = false
                    )
                }
            }

            is NetworkResult.Error -> {
                _state.update {
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
                _state.update {
                    it.copy(
                        todayCheckOuts = result.data,
                        isLoadingCheckOuts = false
                    )
                }
            }

            is NetworkResult.Error -> {
                _state.update {
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
                _state.update {
                    it.copy(
                        weekBookings = result.data,
                        isLoadingWeekBookings = false
                    )
                }
            }

            is NetworkResult.Error -> {
                _state.update {
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
                _state.update {
                    it.copy(
                        pendingReservations = result.data,
                        isLoadingPendingReservations = false
                    )
                }
            }

            is NetworkResult.Error -> {
                _state.update {
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
            when (acceptReservationUseCase(id)) {
                is NetworkResult.Success -> {
                    refreshData()
                }
                is NetworkResult.Error -> {}
                NetworkResult.Loading -> {}
            }
        }
    }

    private fun declineReservation(id: Int) {
        viewModelScope.launch {
            when (declineReservationUseCase(id)) {
                is NetworkResult.Success -> {
                    refreshData()
                }
                is NetworkResult.Error -> {}
                NetworkResult.Loading -> {}
            }
        }
    }

    private fun forceResync() {
        viewModelScope.launch {
            Log.d("DashboardVM", "🔄 FORCE RE-SYNC - Deleting old data...")

            // Sync will delete old data and insert new
            when (val result = syncICalFeedsUseCase()) {
                is NetworkResult.Success -> {
                    Log.d("DashboardVM", "✅ Re-sync complete: ${result.data.size} bookings")
                    loadData() // Refresh UI
                    _toastMessage.emit("Bookings data updated!")
                }
                is NetworkResult.Error -> {
                    Log.e("DashboardVM", "❌ Re-sync failed: ${result.message}")
                }
                NetworkResult.Loading -> {}
            }
        }
    }

}