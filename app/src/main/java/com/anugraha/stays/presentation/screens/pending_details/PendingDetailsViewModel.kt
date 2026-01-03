package com.anugraha.stays.presentation.screens.pending_details

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
class PendingDetailsViewModel @Inject constructor(
    private val reservationRepository: ReservationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PendingDetailsState())
    val state: StateFlow<PendingDetailsState> = _state.asStateFlow()

    fun handleIntent(intent: PendingDetailsIntent) {
        when (intent) {
            is PendingDetailsIntent.LoadReservation -> loadReservation(intent.reservationId)
            PendingDetailsIntent.ShowAcceptDialog -> {
                _state.update { it.copy(showAcceptDialog = true) }
            }
            PendingDetailsIntent.ShowDeclineDialog -> {
                _state.update { it.copy(showDeclineDialog = true) }
            }
            PendingDetailsIntent.DismissDialog -> {
                _state.update {
                    it.copy(
                        showAcceptDialog = false,
                        showDeclineDialog = false
                    )
                }
            }
            PendingDetailsIntent.ConfirmAccept -> acceptReservation()
            PendingDetailsIntent.ConfirmDecline -> declineReservation()
        }
    }

    private fun loadReservation(reservationId: Int) {
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

    private fun acceptReservation() {
        val reservationId = _state.value.reservation?.id ?: return

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isProcessing = true,
                    showAcceptDialog = false
                )
            }

            when (reservationRepository.acceptReservation(reservationId)) {
                is NetworkResult.Success -> {
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            actionSuccess = true
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            error = "Failed to accept reservation"
                        )
                    }
                }
                NetworkResult.Loading -> {}
            }
        }
    }

    private fun declineReservation() {
        val reservationId = _state.value.reservation?.id ?: return

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isProcessing = true,
                    showDeclineDialog = false
                )
            }

            when (reservationRepository.declineReservation(reservationId)) {
                is NetworkResult.Success -> {
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            actionSuccess = true
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            error = "Failed to decline reservation"
                        )
                    }
                }
                NetworkResult.Loading -> {}
            }
        }
    }
}