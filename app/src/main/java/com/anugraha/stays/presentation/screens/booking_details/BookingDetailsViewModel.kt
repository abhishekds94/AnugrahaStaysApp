package com.anugraha.stays.presentation.screens.booking_details

import androidx.lifecycle.viewModelScope
import com.anugraha.stays.domain.repository.ReservationRepository
import com.anugraha.stays.util.BaseViewModel
import com.anugraha.stays.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookingDetailsViewModel @Inject constructor(
    private val reservationRepository: ReservationRepository
) : BaseViewModel<BookingDetailsState, BookingDetailsIntent, BookingDetailsEffect>(BookingDetailsState()) {

    override fun handleIntent(intent: BookingDetailsIntent) {
        when (intent) {
            is BookingDetailsIntent.LoadBooking -> loadBooking(intent.reservationId)
            is BookingDetailsIntent.OpenWhatsApp -> openWhatsApp(intent.phoneNumber)
        }
    }

    private fun openWhatsApp(phoneNumber: String) {
        sendEffect(BookingDetailsEffect.OpenWhatsApp(phoneNumber))
    }

    private fun loadBooking(reservationId: Int) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }

            when (val result = reservationRepository.getReservationById(reservationId)) {
                is NetworkResult.Success -> {
                    updateState {
                        it.copy(
                            reservation = result.data,
                            isLoading = false
                        )
                    }
                }
                is NetworkResult.Error -> {
                    updateState {
                        it.copy(
                            error = result.message,
                            isLoading = false
                        )
                    }
                    sendEffect(BookingDetailsEffect.ShowError(result.message ?: "Failed to load details"))
                }
                NetworkResult.Loading -> {}
            }
        }
    }
}