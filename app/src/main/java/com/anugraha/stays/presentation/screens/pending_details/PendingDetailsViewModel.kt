package com.anugraha.stays.presentation.screens.pending_details

import androidx.lifecycle.viewModelScope
import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.domain.repository.ReservationRepository
import com.anugraha.stays.util.BaseViewModel
import com.anugraha.stays.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PendingDetailsViewModel @Inject constructor(
    private val reservationRepository: ReservationRepository
) : BaseViewModel<PendingDetailsState, PendingDetailsIntent, PendingDetailsEffect>(
    PendingDetailsState()
) {

    override fun handleIntent(intent: PendingDetailsIntent) {
        when (intent) {
            is PendingDetailsIntent.LoadReservation -> loadReservation(intent.reservationId)
            PendingDetailsIntent.ShowAcceptDialog -> updateState { it.copy(showAcceptDialog = true) }
            PendingDetailsIntent.ShowDeclineDialog -> updateState { it.copy(showDeclineDialog = true) }
            PendingDetailsIntent.DismissDialog -> updateState {
                it.copy(
                    showAcceptDialog = false,
                    showDeclineDialog = false
                )
            }

            PendingDetailsIntent.ConfirmAccept -> acceptReservation()
            PendingDetailsIntent.ConfirmDecline -> declineReservation()
            PendingDetailsIntent.ShowWhatsAppDialog -> updateState { it.copy(showWhatsAppDialog = true) }
            PendingDetailsIntent.DismissWhatsAppDialog -> updateState { it.copy(showWhatsAppDialog = false) }
            PendingDetailsIntent.SendWhatsAppMessage -> sendWhatsAppMessage()
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
                    sendEffect(
                        PendingDetailsEffect.ShowError(
                            result.message ?: "Failed to load details"
                        )
                    )
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
                    updateState {
                        it.copy(
                            isProcessing = false,
                            actionSuccess = true,
                            showWhatsAppDialog = true
                        )
                    }
                    sendEffect(PendingDetailsEffect.ShowToast("Reservation accepted"))
                }

                is NetworkResult.Error -> {
                    updateState { it.copy(isProcessing = false, error = result.message) }
                    sendEffect(
                        PendingDetailsEffect.ShowError(
                            result.message ?: "Failed to accept reservation"
                        )
                    )
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
                    sendEffect(
                        PendingDetailsEffect.ShowError(
                            result.message ?: "Failed to decline reservation"
                        )
                    )
                }

                NetworkResult.Loading -> {}
            }
        }
    }

    private fun sendWhatsAppMessage() {
        val reservation = currentState.reservation ?: return
        val phoneNumber = reservation.primaryGuest.phone
        val message = buildWhatsAppMessage(reservation)

        updateState { it.copy(showWhatsAppDialog = false) }
        sendEffect(PendingDetailsEffect.OpenWhatsApp(phoneNumber, message))
        sendEffect(PendingDetailsEffect.NavigateBack)
    }

    private fun buildWhatsAppMessage(reservation: Reservation): String {
        val guestName = reservation.primaryGuest.fullName
        val bookingId = reservation.reservationNumber
        val checkInDate = reservation.checkInDate.toString()
        val checkOutDate = reservation.checkOutDate.toString()
        val totalGuests = reservation.adults + reservation.kids
        val pets = if (reservation.hasPet) "Yes" else "No Pets"
        val checkInTime = reservation.estimatedCheckInTime?.toString() ?: "Not specified"
        val totalPaid = String.format("%.2f", reservation.totalAmount)
        val transactionId = reservation.transactionId ?: "N/A"

        return """
        Hello $guestName 👋
        Thank you for choosing Anugraha Stays! 
        We're happy to confirm your booking. Below are the details for your reference:
        🆔 Booking ID: $bookingId 
        📅 Check-in Date: $checkInDate 
        📅 Check-out Date: $checkOutDate 
        👥 Total Guests: $totalGuests 
        🐾 Pets: $pets 
        ⏰ Approx. Check-in Time: $checkInTime
        💳 Total Amount Paid: ₹$totalPaid 
        🔁 Transaction ID: $transactionId 
        
        If you have any questions, special requests, or need help with directions or check-in, feel free to message us anytime. 
        We look forward to hosting you and hope you have a wonderful stay! 😊
        
        Warm regards, 
        Sheshagiri,
        Anugraha Stays.
        9448628559
                """.trimIndent()
    }
}