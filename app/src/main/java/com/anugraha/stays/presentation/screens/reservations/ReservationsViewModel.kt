package com.anugraha.stays.presentation.screens.reservations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anugraha.stays.domain.model.ReservationStatus
import com.anugraha.stays.domain.usecase.ical.GetExternalBookingsUseCase
import com.anugraha.stays.domain.usecase.reservation.GetReservationsUseCase
import com.anugraha.stays.domain.usecase.reservation.SearchReservationsUseCase
import com.anugraha.stays.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class ReservationsViewModel @Inject constructor(
    private val getReservationsUseCase: GetReservationsUseCase,
    private val searchReservationsUseCase: SearchReservationsUseCase,
    private val getExternalBookingsUseCase: GetExternalBookingsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ReservationsState())
    val state: StateFlow<ReservationsState> = _state.asStateFlow()

    init {
        handleIntent(ReservationsIntent.LoadReservations)
    }

    fun handleIntent(intent: ReservationsIntent) {
        when (intent) {
            ReservationsIntent.LoadReservations -> loadAllReservations()
            is ReservationsIntent.SearchQueryChanged -> updateSearchQuery(intent.query)
            is ReservationsIntent.ToggleMonthExpansion -> toggleMonthExpansion(intent.month)
        }
    }

    private fun loadAllReservations() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, isLoadingExternal = true, error = null) }

            // Load both direct and external bookings in parallel
            val directReservationsJob = launch { loadDirectReservations() }
            val externalBookingsJob = launch { loadExternalBookings() }

            // Wait for both to complete
            directReservationsJob.join()
            externalBookingsJob.join()

            // Combine and group reservations
            combineAndGroupReservations()

            _state.update { it.copy(isLoading = false, isLoadingExternal = false) }
        }
    }

    private suspend fun loadDirectReservations() {
        // Load ALL reservations (no status filter to include past bookings)
        when (val result = getReservationsUseCase(
            page = 1,
            perPage = 1000,
            status = null  // null = get all statuses including past bookings
        )) {
            is NetworkResult.Success -> {
                android.util.Log.d("ReservationsVM", "Loaded ${result.data.size} direct reservations")
                _state.update {
                    it.copy(reservations = result.data)
                }
            }
            is NetworkResult.Error -> {
                android.util.Log.e("ReservationsVM", "Error loading direct reservations: ${result.message}")
                _state.update { it.copy(error = result.message) }
            }
            NetworkResult.Loading -> {}
        }
    }

    private suspend fun loadExternalBookings() {
        try {
            val externalBookings = getExternalBookingsUseCase()
            android.util.Log.d("ReservationsVM", "Loaded ${externalBookings.size} external bookings")

            // Merge with existing reservations
            val currentReservations = _state.value.reservations
            val allReservations = currentReservations + externalBookings

            _state.update {
                it.copy(reservations = allReservations)
            }
        } catch (e: Exception) {
            android.util.Log.e("ReservationsVM", "Error loading external bookings: ${e.message}")
        }
    }

    private fun combineAndGroupReservations() {
        val allReservations = _state.value.reservations

        // Sort by check-in date descending (latest first)
        // BUT within same date, admin-cancelled bookings go to the bottom
        val sortedReservations = allReservations.sortedWith(
            compareByDescending<com.anugraha.stays.domain.model.Reservation> { it.checkInDate }
                .thenBy { if (it.status == ReservationStatus.ADMIN_CANCELLED) 1 else 0 }
        )

        // Group by YearMonth
        val grouped = sortedReservations.groupBy { reservation ->
            YearMonth.from(reservation.checkInDate)
        }.mapValues { (_, reservations) ->
            // Within each month group, sort again to ensure cancelled at bottom
            reservations.sortedWith(
                compareByDescending<com.anugraha.stays.domain.model.Reservation> { it.checkInDate }
                    .thenBy { if (it.status == ReservationStatus.ADMIN_CANCELLED) 1 else 0 }
            )
        }

        // Auto-expand current and next month
        val currentMonth = YearMonth.now()
        val nextMonth = currentMonth.plusMonths(1)
        val autoExpanded = setOf(currentMonth, nextMonth)

        _state.update {
            it.copy(
                filteredReservations = sortedReservations,
                groupedReservations = grouped,
                expandedMonths = autoExpanded
            )
        }

        android.util.Log.d("ReservationsVM", "Grouped ${sortedReservations.size} reservations into ${grouped.size} months")
    }

    private fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }

        if (query.isBlank()) {
            // Reset to all reservations and regroup
            combineAndGroupReservations()
        } else {
            viewModelScope.launch {
                searchReservationsUseCase(query).collect { filtered ->
                    // Sort filtered results with admin-cancelled at bottom
                    val sortedFiltered = filtered.sortedWith(
                        compareByDescending<com.anugraha.stays.domain.model.Reservation> { it.checkInDate }
                            .thenBy { if (it.status == ReservationStatus.ADMIN_CANCELLED) 1 else 0 }
                    )

                    // Regroup filtered results
                    val grouped = sortedFiltered.groupBy { reservation ->
                        YearMonth.from(reservation.checkInDate)
                    }.mapValues { (_, reservations) ->
                        reservations.sortedWith(
                            compareByDescending<com.anugraha.stays.domain.model.Reservation> { it.checkInDate }
                                .thenBy { if (it.status == ReservationStatus.ADMIN_CANCELLED) 1 else 0 }
                        )
                    }

                    // Expand all months when searching
                    val expandedMonths = grouped.keys.toSet()

                    _state.update {
                        it.copy(
                            filteredReservations = sortedFiltered,
                            groupedReservations = grouped,
                            expandedMonths = expandedMonths
                        )
                    }
                }
            }
        }
    }

    private fun toggleMonthExpansion(month: YearMonth) {
        _state.update {
            val newExpanded = if (it.expandedMonths.contains(month)) {
                it.expandedMonths - month
            } else {
                it.expandedMonths + month
            }
            it.copy(expandedMonths = newExpanded)
        }
    }
}