package com.anugraha.stays.presentation.screens.reservations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anugraha.stays.domain.usecase.reservation.GetReservationsUseCase
import com.anugraha.stays.domain.usecase.reservation.SearchReservationsUseCase
import com.anugraha.stays.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReservationsViewModel @Inject constructor(
    private val getReservationsUseCase: GetReservationsUseCase,
    private val searchReservationsUseCase: SearchReservationsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ReservationsState())
    val state: StateFlow<ReservationsState> = _state.asStateFlow()

    init {
        handleIntent(ReservationsIntent.LoadReservations)
    }

    fun handleIntent(intent: ReservationsIntent) {
        when (intent) {
            ReservationsIntent.LoadReservations -> loadReservations()
            is ReservationsIntent.SearchQueryChanged -> updateSearchQuery(intent.query)
        }
    }

    private fun loadReservations() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            when (val result = getReservationsUseCase(perPage = 100)) {
                is NetworkResult.Success -> {
                    _state.update {
                        it.copy(
                            reservations = result.data,
                            filteredReservations = result.data,
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
    }

    private fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }

        if (query.isBlank()) {
            _state.update { it.copy(filteredReservations = it.reservations) }
        } else {
            viewModelScope.launch {
                searchReservationsUseCase(query).collect { filtered ->
                    _state.update { it.copy(filteredReservations = filtered) }
                }
            }
        }
    }
}