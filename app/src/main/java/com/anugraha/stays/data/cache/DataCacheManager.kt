package com.anugraha.stays.data.cache

import com.anugraha.stays.domain.model.Availability
import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.domain.usecase.availability.GetAvailabilityUseCase
import com.anugraha.stays.domain.usecase.ical.GetExternalBookingsUseCase
import com.anugraha.stays.domain.usecase.reservation.GetReservationsUseCase
import com.anugraha.stays.util.NetworkResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central cache manager for preloading and caching data
 * Reduces loading times by maintaining an in-memory cache
 */
@Singleton
class DataCacheManager @Inject constructor(
    private val getReservationsUseCase: GetReservationsUseCase,
    private val getExternalBookingsUseCase: GetExternalBookingsUseCase,
    private val getAvailabilityUseCase: GetAvailabilityUseCase
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Cache for all reservations
    private val _reservationsCache = MutableStateFlow<CacheState<List<Reservation>>>(CacheState.Empty)
    val reservationsCache: StateFlow<CacheState<List<Reservation>>> = _reservationsCache.asStateFlow()

    // Cache for external bookings
    private val _externalBookingsCache = MutableStateFlow<CacheState<List<Reservation>>>(CacheState.Empty)
    val externalBookingsCache: StateFlow<CacheState<List<Reservation>>> = _externalBookingsCache.asStateFlow()

    // Cache for availability (by month)
    private val availabilityCache = mutableMapOf<YearMonth, CacheState<List<Availability>>>()
    private val _currentMonthAvailability = MutableStateFlow<CacheState<List<Availability>>>(CacheState.Empty)
    val currentMonthAvailability: StateFlow<CacheState<List<Availability>>> = _currentMonthAvailability.asStateFlow()

    // Last refresh timestamps
    private var lastReservationsRefresh = 0L
    private var lastExternalBookingsRefresh = 0L
    private val availabilityRefreshTimes = mutableMapOf<YearMonth, Long>()

    // Cache validity duration (120 minutes)
    private val cacheValidityMillis = 120 * 60 * 1000L

    /**
     * Preload all data in the background
     * Should be called from Application.onCreate or Dashboard init
     */
    fun preloadAllData() {
        scope.launch {
            // Preload in parallel
            val jobs = listOf(
                async { preloadReservations() },
                async { preloadExternalBookings() },
                async { preloadCurrentMonthAvailability() }
            )
            jobs.awaitAll()
        }
    }

    /**
     * Get reservations (from cache if available and fresh)
     */
    suspend fun getReservations(forceRefresh: Boolean = false): List<Reservation> {
        // Check cache validity
        if (!forceRefresh && isCacheValid(lastReservationsRefresh)) {
            val cached = _reservationsCache.value
            if (cached is CacheState.Success) {
                return cached.data
            }
        }

        // Fetch fresh data
        return preloadReservations()
    }

    /**
     * Get external bookings (from cache if available and fresh)
     */
    suspend fun getExternalBookings(forceRefresh: Boolean = false): List<Reservation> {
        // Check cache validity
        if (!forceRefresh && isCacheValid(lastExternalBookingsRefresh)) {
            val cached = _externalBookingsCache.value
            if (cached is CacheState.Success) {
                return cached.data
            }
        }

        // Fetch fresh data
        return preloadExternalBookings()
    }

    /**
     * Get all reservations (direct + external)
     */
    suspend fun getAllReservations(forceRefresh: Boolean = false): List<Reservation> {
        val direct = getReservations(forceRefresh)
        val external = getExternalBookings(forceRefresh)
        return direct + external
    }

    /**
     * Get availability for a specific month (from cache if available and fresh)
     */
    suspend fun getAvailability(yearMonth: YearMonth, forceRefresh: Boolean = false): List<Availability> {
        // Check cache validity
        val lastRefresh = availabilityRefreshTimes[yearMonth] ?: 0L
        if (!forceRefresh && isCacheValid(lastRefresh)) {
            val cached = availabilityCache[yearMonth]
            if (cached is CacheState.Success) {
                return cached.data
            }
        }

        // Fetch fresh data
        return preloadAvailability(yearMonth)
    }

    /**
     * Refresh all data
     */
    fun refreshAll() {
        scope.launch {
            val jobs = listOf(
                async { preloadReservations() },
                async { preloadExternalBookings() },
                async { preloadCurrentMonthAvailability() }
            )
            jobs.awaitAll()
        }
    }

    /**
     * Clear all caches
     */
    fun clearCache() {
        _reservationsCache.value = CacheState.Empty
        _externalBookingsCache.value = CacheState.Empty
        _currentMonthAvailability.value = CacheState.Empty
        availabilityCache.clear()
        availabilityRefreshTimes.clear()
        lastReservationsRefresh = 0L
        lastExternalBookingsRefresh = 0L
    }

    // Private helper methods

    private suspend fun preloadReservations(): List<Reservation> {
        _reservationsCache.value = CacheState.Loading

        return when (val result = getReservationsUseCase(page = 1, perPage = 1000, status = null)) {
            is NetworkResult.Success -> {
                val data = result.data ?: emptyList()
                _reservationsCache.value = CacheState.Success(data)
                lastReservationsRefresh = System.currentTimeMillis()
                data
            }
            is NetworkResult.Error -> {
                _reservationsCache.value = CacheState.Error(result.message ?: "Failed to load")
                emptyList()
            }
            NetworkResult.Loading -> emptyList()
        }
    }

    private suspend fun preloadExternalBookings(): List<Reservation> {
        _externalBookingsCache.value = CacheState.Loading

        return try {
            val data = getExternalBookingsUseCase()
            _externalBookingsCache.value = CacheState.Success(data)
            lastExternalBookingsRefresh = System.currentTimeMillis()
            data
        } catch (e: Exception) {
            _externalBookingsCache.value = CacheState.Error(e.message ?: "Failed to load")
            emptyList()
        }
    }

    private suspend fun preloadCurrentMonthAvailability(): List<Availability> {
        val currentMonth = YearMonth.now()
        return preloadAvailability(currentMonth)
    }

    private suspend fun preloadAvailability(yearMonth: YearMonth): List<Availability> {
        availabilityCache[yearMonth] = CacheState.Loading

        return when (val result = getAvailabilityUseCase(yearMonth)) {
            is NetworkResult.Success -> {
                val data = result.data ?: emptyList()
                availabilityCache[yearMonth] = CacheState.Success(data)
                availabilityRefreshTimes[yearMonth] = System.currentTimeMillis()

                // Update current month flow if this is the current month
                if (yearMonth == YearMonth.now()) {
                    _currentMonthAvailability.value = CacheState.Success(data)
                }

                data
            }
            is NetworkResult.Error -> {
                availabilityCache[yearMonth] = CacheState.Error(result.message ?: "Failed to load")
                emptyList()
            }
            NetworkResult.Loading -> emptyList()
        }
    }

    private fun isCacheValid(lastRefreshTime: Long): Boolean {
        return System.currentTimeMillis() - lastRefreshTime < cacheValidityMillis
    }

    /**
     * Preload next and previous months for smoother navigation
     */
    fun preloadAdjacentMonths(currentMonth: YearMonth) {
        scope.launch {
            val jobs = listOf(
                async { preloadAvailability(currentMonth.minusMonths(1)) },
                async { preloadAvailability(currentMonth) },
                async { preloadAvailability(currentMonth.plusMonths(1)) }
            )
            jobs.awaitAll()
        }
    }
}

/**
 * Sealed class representing cache state
 */
sealed class CacheState<out T> {
    object Empty : CacheState<Nothing>()
    object Loading : CacheState<Nothing>()
    data class Success<T>(val data: T) : CacheState<T>()
    data class Error(val message: String) : CacheState<Nothing>()
}