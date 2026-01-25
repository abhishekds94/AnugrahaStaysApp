package com.anugraha.stays.presentation.screens.new_booking

import androidx.lifecycle.viewModelScope
import com.anugraha.stays.domain.usecase.booking.CreateBookingUseCase
import com.anugraha.stays.util.BaseViewModel
import com.anugraha.stays.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewBookingViewModel @Inject constructor(
    private val createBookingUseCase: CreateBookingUseCase
) : BaseViewModel<NewBookingState, NewBookingIntent, NewBookingEffect>(NewBookingState()) {

    override fun handleIntent(intent: NewBookingIntent) {
        when (intent) {
            is NewBookingIntent.GuestNameChanged -> updateState { it.copy(guestName = intent.name, error = null) }
            is NewBookingIntent.GuestEmailChanged -> updateState { it.copy(guestEmail = intent.email) }
            is NewBookingIntent.ContactNumberChanged -> updateState { it.copy(contactNumber = intent.number, error = null) }
            is NewBookingIntent.CheckInDateChanged -> updateState { it.copy(checkInDate = intent.date) }
            is NewBookingIntent.CheckOutDateChanged -> updateState { it.copy(checkOutDate = intent.date) }
            is NewBookingIntent.ArrivalTimeChanged -> updateState { it.copy(arrivalTime = intent.time) }
            is NewBookingIntent.GuestsCountChanged -> updateState { it.copy(guestsCount = intent.count) }
            is NewBookingIntent.PetToggled -> updateState { it.copy(hasPet = intent.hasPet) }
            is NewBookingIntent.RoomIdChanged -> updateState { it.copy(roomId = intent.roomId) }
            is NewBookingIntent.AmountPaidChanged -> updateState { it.copy(amountPaid = intent.amount) }
            is NewBookingIntent.TransactionIdChanged -> updateState { it.copy(transactionId = intent.id) }
            is NewBookingIntent.BookingSourceChanged -> updateState { it.copy(bookingSource = intent.source) }
            NewBookingIntent.CreateBooking -> createBooking()
        }
    }

    private fun createBooking() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }

            val result = createBookingUseCase(
                guestName = currentState.guestName,
                guestEmail = currentState.guestEmail.ifBlank { null },
                contactNumber = currentState.contactNumber,
                checkInDate = currentState.checkInDate.toString(),
                checkOutDate = currentState.checkOutDate.toString(),
                arrivalTime = currentState.arrivalTime.ifBlank { null },
                guestsCount = currentState.guestsCount,
                isPet = currentState.hasPet,
                roomId = currentState.roomId,
                amountPaid = currentState.amountPaid.toDoubleOrNull(),
                transactionId = currentState.transactionId.ifBlank { null },
                bookingSource = currentState.bookingSource
            )

            when (result) {
                is NetworkResult.Success -> {
                    updateState { it.copy(isLoading = false, isSuccess = true) }
                    sendEffect(NewBookingEffect.NavigateBack)
                }
                is NetworkResult.Error -> {
                    updateState { it.copy(isLoading = false, error = result.message) }
                    sendEffect(NewBookingEffect.ShowError(result.message ?: "Failed to create booking"))
                }
                NetworkResult.Loading -> {}
            }
        }
    }
}