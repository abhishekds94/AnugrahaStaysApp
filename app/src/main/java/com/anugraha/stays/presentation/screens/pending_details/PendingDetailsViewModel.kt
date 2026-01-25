package com.anugraha.stays.presentation.screens.pending_details

import androidx.lifecycle.viewModelScope
import com.anugraha.stays.domain.repository.ReservationRepository
import com.anugraha.stays.util.BaseViewModel
import com.anugraha.stays.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PendingDetailsViewModel @Inject constructor(
    private val reservationRepository: ReservationRepository
) : BaseViewModel<PendingDetailsState, PendingDetailsIntent, PendingDetailsEffect>(PendingDetailsState()) {

    override fun handleIntent(intent: PendingDetailsIntent) {
        when (intent) {
            is PendingDetailsIntent.LoadReservation -> loadReservation(intent.reservationId)
            PendingDetailsIntent.ShowAcceptDialog -> updateState { it.copy(showAcceptDialog = true) }
            PendingDetailsIntent.ShowDeclineDialog -> updateState { it.copy(showDeclineDialog = true) }
            PendingDetailsIntent.DismissDialog -> updateState { it.copy(showAcceptDialog = false, showDeclineDialog = false) }
            PendingDetailsIntent.ConfirmAccept -> acceptReservation()
            PendingDetailsIntent.ConfirmDecline -> declineReservation()
        }
    }

    private fun loadReservation(reservationId: Int) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }

            when (val result = reservationRepository.getReservationById(reservationId)) {
                is NetworkResult.Success -> {
                    updateState { it.copy(reservation = result.data, isLoading = false) }
                }
                is NetworkResult.Error -> {
                    updateState { it.copy(error = result.message, isLoading = false) }
                    sendEffect(PendingDetailsEffect.ShowError(result.message ?: "Failed to load details"))
                }
                NetworkResult.Loading -> {}
            }
        }
    }

    private fun acceptReservation() {
        val reservationId = currentState.reservation?.id ?: return
        viewModelScope.launch {
            updateState { it.copy(isProcessing = true, showAcceptDialog = false) }

            when (val result = reservationRepository.acceptReservation(reservationId)) {
                is NetworkResult.Success -> {
                    updateState { it.copy(isProcessing = false, actionSuccess = true) }
                    sendEffect(PendingDetailsEffect.ShowToast("Reservation accepted"))
                    sendEffect(PendingDetailsEffect.NavigateBack)
                }
                is NetworkResult.Error -> {
                    updateState { it.copy(isProcessing = false, error = result.message) }
                    sendEffect(PendingDetailsEffect.ShowError(result.message ?: "Failed to accept reservation"))
                }
                NetworkResult.Loading -> {}
            }
        }
    }

    private fun declineReservation() {
        val reservationId = currentState.reservation?.id ?: return
        viewModelScope.launch {
            updateState { it.copy(isProcessing = true, showDeclineDialog = false) }

            when (val result = reservationRepository.declineReservation(reservationId)) {
                is NetworkResult.Success -> {
                    updateState { it.copy(isProcessing = false, actionSuccess = true) }
                    sendEffect(PendingDetailsEffect.ShowToast("Reservation declined"))
                    sendEffect(PendingDetailsEffect.NavigateBack)
                }
                is NetworkResult.Error -> {
                    updateState { it.copy(isProcessing = false, error = result.message) }
                    sendEffect(PendingDetailsEffect.ShowError(result.message ?: "Failed to decline reservation"))
                }
                NetworkResult.Loading -> {}
            }
        }
    }
}
