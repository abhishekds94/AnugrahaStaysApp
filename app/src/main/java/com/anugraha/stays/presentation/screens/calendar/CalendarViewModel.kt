package com.anugraha.stays.presentation.screens.calendar

import androidx.lifecycle.viewModelScope
import com.anugraha.stays.domain.model.AvailabilityStatus
import com.anugraha.stays.domain.model.ICalConfig
import com.anugraha.stays.domain.repository.ICalSyncRepository
import com.anugraha.stays.domain.usecase.availability.GetAvailabilityUseCase
import com.anugraha.stays.domain.usecase.availability.UpdateAvailabilityUseCase
import com.anugraha.stays.domain.usecase.reservation.DeclineReservationUseCase
import com.anugraha.stays.domain.usecase.reservation.GetReservationsUseCase
import com.anugraha.stays.util.BaseViewModel
import com.anugraha.stays.util.DateUtils
import com.anugraha.stays.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getAvailabilityUseCase: GetAvailabilityUseCase,
    private val updateAvailabilityUseCase: UpdateAvailabilityUseCase,
    private val getReservationsUseCase: GetReservationsUseCase,
    private val declineReservationUseCase: DeclineReservationUseCase,
    private val iCalSyncRepository: ICalSyncRepository
) : BaseViewModel<CalendarState, CalendarIntent, CalendarEffect>(CalendarState()) {

    init {
        loadAllData()
    }

    private fun loadAllData() {
        viewModelScope.launch {
            updateState {
                it.copy(
                    isLoading = true,
                    syncFailureMessage = null,
                    failedSources = emptyList()
                )
            }

            val availabilityDeferred = async { getMonthAvailability(YearMonth.from(DateUtils.now())) }
            val bookingsDeferred = async { getBookings() }
            val syncDeferred = async { syncExternalBookings() }

            val availability = availabilityDeferred.await()
            val bookings = bookingsDeferred.await()
            syncDeferred.await()

            updateState {
                it.copy(
                    availabilities = availability,
                    reservations = bookings,
                    isLoading = false
                )
            }
        }
    }

    override fun handleIntent(intent: CalendarIntent) {
        when (intent) {
            is CalendarIntent.LoadMonth -> {
                viewModelScope.launch {
                    val availability = getMonthAvailability(intent.yearMonth)
                    updateState { it.copy(currentMonth = intent.yearMonth, availabilities = availability) }
                }
            }
            is CalendarIntent.DateSelected -> selectDate(intent.date)
            is CalendarIntent.ToggleAvailability -> toggleAvailability(intent.date)
            is CalendarIntent.LoadBookings -> {
                viewModelScope.launch {
                    val bookings = getBookings()
                    updateState { it.copy(reservations = bookings) }
                }
            }
            is CalendarIntent.BlockDate -> blockDate(intent.date)
            is CalendarIntent.OpenDate -> openDate(intent.date)
            is CalendarIntent.CancelBooking -> cancelBooking(intent.reservationId, intent.date)
            CalendarIntent.PreviousMonth -> {
                val prevMonth = currentState.currentMonth.minusMonths(1)
                handleIntent(CalendarIntent.LoadMonth(prevMonth))
            }
            CalendarIntent.NextMonth -> {
                val nextMonth = currentState.currentMonth.plusMonths(1)
                handleIntent(CalendarIntent.LoadMonth(nextMonth))
            }
        }
    }

    private suspend fun getMonthAvailability(yearMonth: YearMonth): List<com.anugraha.stays.domain.model.Availability> {
        return when (val result = getAvailabilityUseCase(yearMonth)) {
            is NetworkResult.Success -> result.data ?: emptyList()
            is NetworkResult.Error -> {
                sendEffect(CalendarEffect.ShowError(result.message ?: "Failed to load availability"))
                emptyList()
            }
            NetworkResult.Loading -> emptyList()
        }
    }

    private suspend fun getBookings(): List<com.anugraha.stays.domain.model.Reservation> {
        return when (val result = getReservationsUseCase(page = 1, perPage = 1000, status = null)) {
            is NetworkResult.Success -> result.data ?: emptyList()
            is NetworkResult.Error -> {
                sendEffect(CalendarEffect.ShowError(result.message ?: "Failed to load bookings"))
                emptyList()
            }
            NetworkResult.Loading -> emptyList()
        }
    }

    private suspend fun syncExternalBookings() {
        val configs = ICalConfig.getDefaultConfigs()
        val (result, sourceStatuses) = iCalSyncRepository.syncICalFeedsDetailed(configs)

        val failedSources = sourceStatuses.filter { !it.isSuccess }.map { it.source }

        val failureMessage = when {
            failedSources.isEmpty() -> null
            failedSources.size == 1 -> "${failedSources.first().getDisplayName()} failed"
            failedSources.size == 2 -> "${failedSources[0].getDisplayName()} and ${failedSources[1].getDisplayName()} failed"
            else -> "Multiple external sources failed"
        }

        updateState {
            it.copy(
                failedSources = failedSources,
                syncFailureMessage = failureMessage
            )
        }

        if (result is NetworkResult.Success) {
            val bookings = getBookings()
            updateState { it.copy(reservations = bookings) }
            if (failureMessage != null) {
                sendEffect(CalendarEffect.ShowToast(failureMessage))
            }
        }
    }

    private fun selectDate(date: LocalDate) {
        val availability = currentState.availabilities.find { it.date == date }
        updateState {
            it.copy(
                selectedDate = date,
                selectedDateAvailability = availability,
                actionSuccess = false,
                actionError = null
            )
        }
    }

    private fun toggleAvailability(date: LocalDate) {
        viewModelScope.launch {
            val currentAvailability = currentState.selectedDateAvailability
            val newStatus = if (currentAvailability?.status == AvailabilityStatus.CLOSED) {
                AvailabilityStatus.OPEN
            } else {
                AvailabilityStatus.CLOSED
            }

            when (val result = updateAvailabilityUseCase(date, newStatus)) {
                is NetworkResult.Success -> {
                    val availability = getMonthAvailability(currentState.currentMonth)
                    updateState { it.copy(availabilities = availability) }
                }
                is NetworkResult.Error -> {
                    sendEffect(CalendarEffect.ShowError(result.message ?: "Update failed"))
                }
                NetworkResult.Loading -> {}
            }
        }
    }

    private fun blockDate(date: LocalDate) {
        performAction { updateAvailabilityUseCase(date, AvailabilityStatus.CLOSED) }
    }

    private fun openDate(date: LocalDate) {
        performAction { updateAvailabilityUseCase(date, AvailabilityStatus.OPEN) }
    }

    private fun cancelBooking(reservationId: Int, date: LocalDate) {
        performAction { declineReservationUseCase(reservationId) }
    }

    private fun performAction(action: suspend () -> NetworkResult<*>) {
        viewModelScope.launch {
            updateState {
                it.copy(
                    isActionInProgress = true,
                    actionError = null,
                    actionSuccess = false
                )
            }

            when (val result = action()) {
                is NetworkResult.Success -> {
                    updateState {
                        it.copy(
                            isActionInProgress = false,
                            actionSuccess = true
                        )
                    }
                    delay(500)
                    val availability = getMonthAvailability(currentState.currentMonth)
                    val bookings = getBookings()
                    updateState {
                        it.copy(
                            availabilities = availability,
                            reservations = bookings
                        )
                    }
                }
                is NetworkResult.Error -> {
                    updateState {
                        it.copy(
                            isActionInProgress = false,
                            actionError = result.message
                        )
                    }
                    sendEffect(CalendarEffect.ShowError(result.message ?: "Action failed"))
                }
                NetworkResult.Loading -> {}
            }
        }
    }
}
