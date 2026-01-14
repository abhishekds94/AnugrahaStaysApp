package com.anugraha.stays.presentation.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anugraha.stays.domain.model.AvailabilityStatus
import com.anugraha.stays.domain.usecase.availability.GetAvailabilityUseCase
import com.anugraha.stays.domain.usecase.availability.UpdateAvailabilityUseCase
import com.anugraha.stays.domain.usecase.reservation.DeclineReservationUseCase
import com.anugraha.stays.domain.usecase.reservation.GetReservationsUseCase
import com.anugraha.stays.util.DateUtils
import com.anugraha.stays.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject
import com.anugraha.stays.domain.model.ICalSource
import com.anugraha.stays.domain.repository.ICalSyncRepository

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getAvailabilityUseCase: GetAvailabilityUseCase,
    private val updateAvailabilityUseCase: UpdateAvailabilityUseCase,
    private val getReservationsUseCase: GetReservationsUseCase,
    private val declineReservationUseCase: DeclineReservationUseCase,
    private val iCalSyncRepository: ICalSyncRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarState())
    val state: StateFlow<CalendarState> = _state.asStateFlow()

    init {
        loadAllData()
    }

    private fun loadAllData() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    syncFailureMessage = null,
                    failedSources = emptyList()
                )
            }

            val availabilityJob = launch { loadMonth(YearMonth.from(DateUtils.now())) }
            val bookingsJob = launch { loadBookings() }
            val externalJob = launch { syncExternalBookings() }

            availabilityJob.join()
            bookingsJob.join()
            externalJob.join()

            _state.update { it.copy(isLoading = false) }
        }
    }

    fun handleIntent(intent: CalendarIntent) {
        when (intent) {
            is CalendarIntent.LoadMonth -> {
                viewModelScope.launch {
                    loadMonth(intent.yearMonth)
                }
            }
            is CalendarIntent.DateSelected -> selectDate(intent.date)
            is CalendarIntent.ToggleAvailability -> toggleAvailability(intent.date)
            is CalendarIntent.LoadBookings -> {
                viewModelScope.launch {
                    loadBookings()
                }
            }
            is CalendarIntent.BlockDate -> blockDate(intent.date)
            is CalendarIntent.OpenDate -> openDate(intent.date)
            is CalendarIntent.CancelBooking -> cancelBooking(intent.reservationId, intent.date)
            CalendarIntent.PreviousMonth -> {
                val prevMonth = _state.value.currentMonth.minusMonths(1)
                handleIntent(CalendarIntent.LoadMonth(prevMonth))
            }
            CalendarIntent.NextMonth -> {
                val nextMonth = _state.value.currentMonth.plusMonths(1)
                handleIntent(CalendarIntent.LoadMonth(nextMonth))
            }
        }
    }

    private suspend fun loadMonth(yearMonth: YearMonth) {
        _state.update { it.copy(currentMonth = yearMonth) }

        when (val result = getAvailabilityUseCase(yearMonth)) {
            is NetworkResult.Success -> {
                _state.update {
                    it.copy(
                        availabilities = result.data,
                        actionSuccess = false
                    )
                }
                android.util.Log.d("CalendarViewModel", "Loaded ${result.data.size} availabilities for $yearMonth")
            }
            is NetworkResult.Error -> {
                _state.update {
                    it.copy(error = result.message)
                }
            }
            NetworkResult.Loading -> {}
        }
    }

    private suspend fun loadBookings() {
        when (val result = getReservationsUseCase(
            page = 1,
            perPage = 1000,
            status = null
        )) {
            is NetworkResult.Success -> {
                _state.update {
                    it.copy(
                        reservations = result.data,
                        error = null
                    )
                }
            }
            is NetworkResult.Error -> {
                _state.update {
                    it.copy(error = result.message)
                }
            }
            NetworkResult.Loading -> {}
        }
    }

    private suspend fun syncExternalBookings() {
        android.util.Log.d("CalendarViewModel", "🔄 Starting external bookings sync")

        val configs = com.anugraha.stays.domain.model.ICalConfig.getDefaultConfigs()
        val (result, sourceStatuses) = iCalSyncRepository.syncICalFeedsDetailed(configs)

        // Determine which sources failed
        val failedSources = sourceStatuses
            .filter { !it.isSuccess }
            .map { it.source }

        // Create failure message
        val failureMessage = when {
            failedSources.isEmpty() -> null
            failedSources.size == 1 -> "${failedSources.first().getDisplayName()} failed"
            failedSources.size == 2 -> {
                "${failedSources[0].getDisplayName()} and ${failedSources[1].getDisplayName()} failed"
            }
            else -> "Multiple external sources failed"
        }

        when (result) {
            is NetworkResult.Success -> {
                android.util.Log.d("CalendarViewModel", "✅ External sync completed: ${result.data.size} bookings")
                _state.update {
                    it.copy(
                        failedSources = failedSources,
                        syncFailureMessage = failureMessage
                    )
                }

                // Reload bookings to include external ones
                loadBookings()
            }
            is NetworkResult.Error -> {
                android.util.Log.e("CalendarViewModel", "❌ External sync failed: ${result.message}")
                _state.update {
                    it.copy(error = result.message)
                }
            }
            NetworkResult.Loading -> {}
        }
    }

    private fun selectDate(date: java.time.LocalDate) {
        val availability = _state.value.availabilities.find { it.date == date }
        _state.update {
            it.copy(
                selectedDate = date,
                selectedDateAvailability = availability,
                // Reset action states when selecting a new date
                actionSuccess = false,
                actionError = null
            )
        }
    }

    private fun toggleAvailability(date: java.time.LocalDate) {
        viewModelScope.launch {
            val currentAvailability = _state.value.selectedDateAvailability
            val newStatus = if (currentAvailability?.status == AvailabilityStatus.CLOSED) {
                AvailabilityStatus.OPEN
            } else {
                AvailabilityStatus.CLOSED
            }

            when (updateAvailabilityUseCase(date, newStatus)) {
                is NetworkResult.Success -> {
                    loadMonth(_state.value.currentMonth)
                }
                is NetworkResult.Error -> {
                    // Handle error
                }
                NetworkResult.Loading -> {}
            }
        }
    }

    private fun blockDate(date: java.time.LocalDate) {
        viewModelScope.launch {
            android.util.Log.d("CalendarViewModel", "========== BLOCK DATE STARTED ==========")
            android.util.Log.d("CalendarViewModel", "Date to block: $date")
            android.util.Log.d("CalendarViewModel", "Calling updateAvailabilityUseCase with status: CLOSED")

            _state.update {
                it.copy(
                    isActionInProgress = true,
                    actionError = null,
                    actionSuccess = false
                )
            }

            when (val result = updateAvailabilityUseCase(date, AvailabilityStatus.CLOSED)) {
                is NetworkResult.Success -> {
                    android.util.Log.d("CalendarViewModel", "✅ Block date SUCCESS")
                    _state.update {
                        it.copy(
                            isActionInProgress = false,
                            actionSuccess = true
                        )
                    }
                    kotlinx.coroutines.delay(500)
                    // Refresh calendar data
                    android.util.Log.d("CalendarViewModel", "Refreshing calendar data...")
                    launch { loadMonth(_state.value.currentMonth) }
                    launch { loadBookings() }
                    android.util.Log.d("CalendarViewModel", "========== BLOCK DATE COMPLETED ==========")
                }
                is NetworkResult.Error -> {
                    android.util.Log.e("CalendarViewModel", "❌ Block date FAILED: ${result.message}")
                    _state.update {
                        it.copy(
                            isActionInProgress = false,
                            actionError = result.message
                        )
                    }
                    android.util.Log.d("CalendarViewModel", "========== BLOCK DATE FAILED ==========")
                }
                NetworkResult.Loading -> {
                    android.util.Log.d("CalendarViewModel", "Block date loading state...")
                }
            }
        }
    }

    private fun openDate(date: java.time.LocalDate) {
        viewModelScope.launch {
            android.util.Log.d("CalendarViewModel", "========== OPEN DATE STARTED ==========")
            android.util.Log.d("CalendarViewModel", "Date to open: $date")
            android.util.Log.d("CalendarViewModel", "Calling updateAvailabilityUseCase with status: OPEN")

            _state.update {
                it.copy(
                    isActionInProgress = true,
                    actionError = null,
                    actionSuccess = false
                )
            }

            when (val result = updateAvailabilityUseCase(date, AvailabilityStatus.OPEN)) {
                is NetworkResult.Success -> {
                    android.util.Log.d("CalendarViewModel", "✅ Open date SUCCESS")
                    _state.update {
                        it.copy(
                            isActionInProgress = false,
                            actionSuccess = true
                        )
                    }
                    kotlinx.coroutines.delay(500)
                    // Refresh calendar data
                    android.util.Log.d("CalendarViewModel", "Refreshing calendar data...")
                    launch { loadMonth(_state.value.currentMonth) }
                    launch { loadBookings() }
                    android.util.Log.d("CalendarViewModel", "========== OPEN DATE COMPLETED ==========")
                }
                is NetworkResult.Error -> {
                    android.util.Log.e("CalendarViewModel", "❌ Open date FAILED: ${result.message}")
                    _state.update {
                        it.copy(
                            isActionInProgress = false,
                            actionError = result.message
                        )
                    }
                    android.util.Log.d("CalendarViewModel", "========== OPEN DATE FAILED ==========")
                }
                NetworkResult.Loading -> {
                    android.util.Log.d("CalendarViewModel", "Open date loading state...")
                }
            }
        }
    }

    private fun cancelBooking(reservationId: Int, date: java.time.LocalDate) {
        viewModelScope.launch {
            android.util.Log.d("CalendarViewModel", "========== CANCEL BOOKING STARTED ==========")
            android.util.Log.d("CalendarViewModel", "Reservation ID to cancel: $reservationId")
            android.util.Log.d("CalendarViewModel", "Date: $date")
            android.util.Log.d("CalendarViewModel", "Calling declineReservationUseCase...")

            _state.update {
                it.copy(
                    isActionInProgress = true,
                    actionError = null,
                    actionSuccess = false
                )
            }

            when (val result = declineReservationUseCase(reservationId)) {
                is NetworkResult.Success -> {
                    android.util.Log.d("CalendarViewModel", "✅ Cancel booking SUCCESS")
                    _state.update {
                        it.copy(
                            isActionInProgress = false,
                            actionSuccess = true
                        )
                    }
                    kotlinx.coroutines.delay(500)
                    // Refresh calendar data
                    android.util.Log.d("CalendarViewModel", "Refreshing calendar data...")
                    launch { loadMonth(_state.value.currentMonth) }
                    launch { loadBookings() }
                    android.util.Log.d("CalendarViewModel", "========== CANCEL BOOKING COMPLETED ==========")
                }
                is NetworkResult.Error -> {
                    android.util.Log.e("CalendarViewModel", "❌ Cancel booking FAILED: ${result.message}")
                    _state.update {
                        it.copy(
                            isActionInProgress = false,
                            actionError = result.message
                        )
                    }
                    android.util.Log.d("CalendarViewModel", "========== CANCEL BOOKING FAILED ==========")
                }
                NetworkResult.Loading -> {
                    android.util.Log.d("CalendarViewModel", "Cancel booking loading state...")
                }
            }
        }
    }
}