package com.anugraha.stays.presentation.screens.reservations

import androidx.lifecycle.viewModelScope
import com.anugraha.stays.data.cache.DataCacheManager
import com.anugraha.stays.domain.model.BookingSource
import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.domain.model.ReservationStatus
import com.anugraha.stays.domain.usecase.ical.GetExternalBookingsUseCase
import com.anugraha.stays.domain.usecase.reservation.GetReservationsUseCase
import com.anugraha.stays.domain.usecase.reservation.SearchReservationsUseCase
import com.anugraha.stays.util.AdvancedBookingDeduplicator
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
    private val dataCacheManager: DataCacheManager,
    private val advancedDeduplicator: AdvancedBookingDeduplicator
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
                android.util.Log.d("ReservationsVM", "=== Showing Cached Data ===")
                android.util.Log.d("ReservationsVM", "Cached reservations (before dedup): ${cachedReservations.size}")

                // Apply advanced deduplication to cached data
                val deduplicatedCache = advancedDeduplicator.deduplicateAllBookings(cachedReservations)
                android.util.Log.d("ReservationsVM", "Cached reservations (after dedup): ${deduplicatedCache.size}")

                // Show deduplicated cached data immediately
                updateState { it.copy(reservations = deduplicatedCache, isLoading = false, isLoadingExternal = false) }
                combineAndGroupReservations(deduplicatedCache)
            }

            // Then fetch fresh data in background
            val directReservationsDeferred = async { getDirectReservations() }
            val externalBookingsDeferred = async { getExternalBookings() }

            val directReservations = directReservationsDeferred.await()
            val externalBookings = externalBookingsDeferred.await()

            android.util.Log.d("ReservationsVM", "=== Fresh Data Loaded ===")
            android.util.Log.d("ReservationsVM", "Direct reservations: ${directReservations.size}")
            android.util.Log.d("ReservationsVM", "External bookings: ${externalBookings.size}")

            // Combine all bookings
            val allReservations = directReservations + externalBookings
            android.util.Log.d("ReservationsVM", "Combined total (before advanced dedup): ${allReservations.size}")

            // Apply advanced deduplication
            // This removes both cross-platform duplicates AND direct booking conflicts
            val deduplicatedReservations = advancedDeduplicator.deduplicateAllBookings(allReservations)
            android.util.Log.d("ReservationsVM", "After advanced dedup: ${deduplicatedReservations.size}")
            android.util.Log.d("ReservationsVM", "Removed ${allReservations.size - deduplicatedReservations.size} total duplicates")

            updateState { it.copy(reservations = deduplicatedReservations) }

            combineAndGroupReservations(deduplicatedReservations)

            updateState { it.copy(isLoading = false, isLoadingExternal = false) }
        }
    }

    private suspend fun getDirectReservations(): List<Reservation> {
        return try {
            val all = dataCacheManager.getReservations(forceRefresh = true)

            // CRITICAL FIX: Filter out external bookings from direct API
            // External bookings come separately from iCal sync
            val filtered = all.filter {
                it.bookingSource != BookingSource.AIRBNB &&
                        it.bookingSource != BookingSource.BOOKING_COM
            }

            android.util.Log.d("ReservationsVM", "Direct API returned ${all.size} total, filtered out ${all.size - filtered.size} external, keeping ${filtered.size} direct")

            filtered
        } catch (e: Exception) {
            when (val result = getReservationsUseCase(page = 1, perPage = 1000, status = null)) {
                is NetworkResult.Success -> {
                    val all = result.data ?: emptyList()
                    // Also filter here
                    all.filter {
                        it.bookingSource != BookingSource.AIRBNB &&
                                it.bookingSource != BookingSource.BOOKING_COM
                    }
                }
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