package com.anugraha.stays.presentation.screens.reservations

import androidx.lifecycle.viewModelScope
import com.anugraha.stays.data.cache.DataCacheManager
import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.domain.model.ReservationStatus
import com.anugraha.stays.domain.usecase.ical.GetExternalBookingsUseCase
import com.anugraha.stays.domain.usecase.reservation.GetReservationsUseCase
import com.anugraha.stays.domain.usecase.reservation.SearchReservationsUseCase
import com.anugraha.stays.util.BaseViewModel
import com.anugraha.stays.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class ReservationsViewModel @Inject constructor(
    private val getReservationsUseCase: GetReservationsUseCase,
    private val searchReservationsUseCase: SearchReservationsUseCase,
    private val getExternalBookingsUseCase: GetExternalBookingsUseCase,
    private val dataCacheManager: DataCacheManager
) : BaseViewModel<ReservationsState, ReservationsIntent, ReservationsEffect>(ReservationsState()) {

    init {
        handleIntent(ReservationsIntent.LoadReservations)
    }

    override fun handleIntent(intent: ReservationsIntent) {
        when (intent) {
            ReservationsIntent.LoadReservations -> loadAllReservations()
            is ReservationsIntent.SearchQueryChanged -> updateSearchQuery(intent.query)
            is ReservationsIntent.ToggleMonthExpansion -> toggleMonthExpansion(intent.month)
        }
    }

    private fun loadAllReservations() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, isLoadingExternal = true, error = null) }

            // Try to use cached data first for instant loading
            val cachedReservations = dataCacheManager.getAllReservations(forceRefresh = false)

            if (cachedReservations.isNotEmpty()) {
                // Show cached data immediately
                updateState { it.copy(reservations = cachedReservations, isLoading = false, isLoadingExternal = false) }
                combineAndGroupReservations(cachedReservations)
            }

            // Then fetch fresh data in background
            val directReservationsDeferred = async { getDirectReservations() }
            val externalBookingsDeferred = async { getExternalBookings() }

            val directReservations = directReservationsDeferred.await()
            val externalBookings = externalBookingsDeferred.await()

            val allReservations = directReservations + externalBookings
            updateState { it.copy(reservations = allReservations) }

            combineAndGroupReservations(allReservations)

            updateState { it.copy(isLoading = false, isLoadingExternal = false) }
        }
    }

    private suspend fun getDirectReservations(): List<Reservation> {
        return try {
            dataCacheManager.getReservations(forceRefresh = true)
        } catch (e: Exception) {
            when (val result = getReservationsUseCase(page = 1, perPage = 1000, status = null)) {
                is NetworkResult.Success -> result.data ?: emptyList()
                is NetworkResult.Error -> {
                    sendEffect(ReservationsEffect.ShowError(result.message ?: "Failed to load reservations"))
                    emptyList()
                }
                NetworkResult.Loading -> emptyList()
            }
        }
    }

    private suspend fun getExternalBookings(): List<Reservation> {
        return try {
            dataCacheManager.getExternalBookings(forceRefresh = true)
        } catch (e: Exception) {
            try {
                getExternalBookingsUseCase()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private fun combineAndGroupReservations(allReservations: List<Reservation>) {
        val sortedReservations = allReservations.sortedWith(
            compareByDescending<Reservation> { it.checkInDate }
                .thenBy { if (it.status == ReservationStatus.ADMIN_CANCELLED) 1 else 0 }
        )

        val grouped = sortedReservations.groupBy { reservation ->
            YearMonth.from(reservation.checkInDate)
        }.mapValues { (_, reservations) ->
            reservations.sortedWith(
                compareByDescending<Reservation> { it.checkInDate }
                    .thenBy { if (it.status == ReservationStatus.ADMIN_CANCELLED) 1 else 0 }
            )
        }

        updateState {
            it.copy(
                filteredReservations = sortedReservations,
                groupedReservations = grouped,
                expandedMonths = emptySet()
            )
        }
    }

    private fun updateSearchQuery(query: String) {
        updateState { it.copy(searchQuery = query) }

        if (query.isBlank()) {
            combineAndGroupReservations(currentState.reservations)
        } else {
            viewModelScope.launch {
                searchReservationsUseCase(query).collectLatest { filtered ->
                    val sortedFiltered = filtered.sortedWith(
                        compareByDescending<Reservation> { it.checkInDate }
                            .thenBy { if (it.status == ReservationStatus.ADMIN_CANCELLED) 1 else 0 }
                    )

                    val grouped = sortedFiltered.groupBy { reservation ->
                        YearMonth.from(reservation.checkInDate)
                    }.mapValues { (_, reservations) ->
                        reservations.sortedWith(
                            compareByDescending<Reservation> { it.checkInDate }
                                .thenBy { if (it.status == ReservationStatus.ADMIN_CANCELLED) 1 else 0 }
                        )
                    }

                    updateState {
                        it.copy(
                            filteredReservations = sortedFiltered,
                            groupedReservations = grouped,
                            expandedMonths = grouped.keys.toSet()
                        )
                    }
                }
            }
        }
    }

    private fun toggleMonthExpansion(month: YearMonth) {
        updateState {
            val isCurrentlyExpanded = it.expandedMonths.contains(month)
            val newExpanded = if (isCurrentlyExpanded) {
                it.expandedMonths - month
            } else {
                setOf(month)
            }
            it.copy(expandedMonths = newExpanded)
        }
    }
}