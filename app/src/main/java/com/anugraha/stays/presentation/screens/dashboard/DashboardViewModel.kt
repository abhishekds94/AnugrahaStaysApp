package com.anugraha.stays.presentation.screens.dashboard

import androidx.lifecycle.viewModelScope
import com.anugraha.stays.data.local.preferences.DebugPreferences
import com.anugraha.stays.domain.model.CheckIn
import com.anugraha.stays.domain.model.CheckOut
import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.domain.model.WeekBooking
import com.anugraha.stays.domain.usecase.dashboard.GetPendingReservationsUseCase
import com.anugraha.stays.domain.usecase.dashboard.GetTodayCheckInsUseCase
import com.anugraha.stays.domain.usecase.dashboard.GetTodayCheckOutsUseCase
import com.anugraha.stays.domain.usecase.dashboard.GetWeekBookingsUseCase
import com.anugraha.stays.domain.usecase.ical.SyncICalFeedsUseCase
import com.anugraha.stays.domain.usecase.reservation.AcceptReservationUseCase
import com.anugraha.stays.domain.usecase.reservation.DeclineReservationUseCase
import com.anugraha.stays.util.BaseViewModel
import com.anugraha.stays.util.DebugDataProvider
import com.anugraha.stays.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
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
    private val syncICalFeedsUseCase: SyncICalFeedsUseCase,
    private val debugPreferences: DebugPreferences
) : BaseViewModel<DashboardState, DashboardIntent, DashboardEffect>(DashboardState()) {

    init {
        // Observe debug mode changes
        viewModelScope.launch {
            debugPreferences.useDebugData.collect { useDebugData ->
                updateState { it.copy(useDebugData = useDebugData) }
                // Reload data when debug mode changes
                if (useDebugData) {
                    loadDebugData()
                } else {
                    handleIntent(DashboardIntent.LoadData)
                }
            }
        }
    }

    override fun handleIntent(intent: DashboardIntent) {
        when (intent) {
            is DashboardIntent.LoadData -> loadData()
            is DashboardIntent.RefreshData -> refreshData()
            is DashboardIntent.SyncExternalBookings -> syncExternalBookings()
            is DashboardIntent.AcceptReservation -> acceptReservation(intent.id)
            is DashboardIntent.DeclineReservation -> declineReservation(intent.id)
            is DashboardIntent.ForceResync -> forceResync()
            is DashboardIntent.ToggleDebugMode -> toggleDebugMode()
        }
    }

    private fun toggleDebugMode() {
        viewModelScope.launch {
            debugPreferences.toggleDebugMode()
        }
    }

    private fun loadDebugData() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }

            // Simulate a slight delay for realism
            kotlinx.coroutines.delay(300)

            updateState {
                it.copy(
                    todayCheckIns = DebugDataProvider.getTodayCheckIns(),
                    todayCheckOuts = DebugDataProvider.getTodayCheckOuts(),
                    weekBookings = DebugDataProvider.getUpcomingCheckIns(),
                    pendingReservations = DebugDataProvider.getPendingReservations(),
                    isLoading = false,
                    isLoadingCheckIns = false,
                    isLoadingCheckOuts = false,
                    isLoadingWeekBookings = false,
                    isLoadingPendingReservations = false,
                    error = null
                )
            }
        }
    }

    private fun syncExternalBookings() {
        // Don't sync in debug mode
        if (currentState.useDebugData) {
            sendEffect(DashboardEffect.ShowToast("Sync disabled in debug mode"))
            return
        }

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
        // Use debug data if enabled
        viewModelScope.launch {
            val useDebugData = debugPreferences.useDebugData.first()
            if (useDebugData) {
                loadDebugData()
                return@launch
            }
        }

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
        // Use debug data if enabled
        viewModelScope.launch {
            val useDebugData = debugPreferences.useDebugData.first()
            if (useDebugData) {
                loadDebugData()
                return@launch
            }
        }

        viewModelScope.launch {
            updateState { it.copy(isRefreshing = true) }

            listOf(
                async { loadTodayCheckIns() },
                async { loadTodayCheckOuts() },
                async { loadWeekBookings() },
                async { loadPendingReservations() }
            ).awaitAll()

            updateState { it.copy(isRefreshing = false) }
        }
    }

    private fun forceResync() {
        // Don't sync in debug mode
        if (currentState.useDebugData) {
            sendEffect(DashboardEffect.ShowToast("Sync disabled in debug mode"))
            return
        }

        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }

            when (val syncResult = syncICalFeedsUseCase()) {
                is NetworkResult.Success -> {
                    sendEffect(DashboardEffect.ShowToast("Re-sync completed successfully"))

                    listOf(
                        async { loadTodayCheckIns() },
                        async { loadTodayCheckOuts() },
                        async { loadWeekBookings() },
                        async { loadPendingReservations() }
                    ).awaitAll()

                    updateState { it.copy(isLoading = false) }
                }
                is NetworkResult.Error -> {
                    updateState { it.copy(isLoading = false) }
                    sendEffect(DashboardEffect.ShowError(syncResult.message ?: "Re-sync failed"))
                }
                NetworkResult.Loading -> {}
            }
        }
    }

    private suspend fun loadTodayCheckIns() {
        val result = getTodayCheckInsUseCase()
        when (result) {
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
                        isLoadingCheckIns = false,
                        error = result.message
                    )
                }
            }
            NetworkResult.Loading -> {}
        }
    }

    private suspend fun loadTodayCheckOuts() {
        val result = getTodayCheckOutsUseCase()
        when (result) {
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
                        isLoadingCheckOuts = false,
                        error = result.message
                    )
                }
            }
            NetworkResult.Loading -> {}
        }
    }

    private suspend fun loadWeekBookings() {
        val result = getWeekBookingsUseCase()
        when (result) {
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
                        isLoadingWeekBookings = false,
                        error = result.message
                    )
                }
            }
            NetworkResult.Loading -> {}
        }
    }

    private suspend fun loadPendingReservations() {
        val result = getPendingReservationsUseCase()
        when (result) {
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
                        isLoadingPendingReservations = false,
                        error = result.message
                    )
                }
            }
            NetworkResult.Loading -> {}
        }
    }

    private fun acceptReservation(id: Int) {
        if (currentState.useDebugData) {
            sendEffect(DashboardEffect.ShowToast("Actions disabled in debug mode"))
            return
        }

        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }

            when (val result = acceptReservationUseCase(id)) {
                is NetworkResult.Success -> {
                    sendEffect(DashboardEffect.ShowToast("Reservation accepted"))
                    loadData()
                }
                is NetworkResult.Error -> {
                    updateState { it.copy(isLoading = false) }
                    sendEffect(DashboardEffect.ShowError(result.message ?: "Failed to accept"))
                }
                NetworkResult.Loading -> {}
            }
        }
    }

    private fun declineReservation(id: Int) {
        if (currentState.useDebugData) {
            sendEffect(DashboardEffect.ShowToast("Actions disabled in debug mode"))
            return
        }

        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }

            when (val result = declineReservationUseCase(id)) {
                is NetworkResult.Success -> {
                    sendEffect(DashboardEffect.ShowToast("Reservation declined"))
                    loadData()
                }
                is NetworkResult.Error -> {
                    updateState { it.copy(isLoading = false) }
                    sendEffect(DashboardEffect.ShowError(result.message ?: "Failed to decline"))
                }
                NetworkResult.Loading -> {}
            }
        }
    }
}