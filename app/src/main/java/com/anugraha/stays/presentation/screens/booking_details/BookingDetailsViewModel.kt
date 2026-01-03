package com.anugraha.stays.presentation.screens.booking_details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anugraha.stays.domain.repository.ReservationRepository
import com.anugraha.stays.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookingDetailsViewModel @Inject constructor(
    private val reservationRepository: ReservationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BookingDetailsState())
    val state: StateFlow<BookingDetailsState> = _state.asStateFlow()

    fun handleIntent(intent: BookingDetailsIntent) {
        when (intent) {
            is BookingDetailsIntent.LoadBooking -> loadBooking(intent.reservationId)
        }
    }

    private fun loadBooking(reservationId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            when (val result = reservationRepository.getReservationById(reservationId)) {
                is NetworkResult.Success -> {
                    _state.update {
                        it.copy(
                            reservation = result.data,
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
}