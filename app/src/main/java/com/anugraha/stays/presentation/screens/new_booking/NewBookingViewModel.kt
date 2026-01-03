package com.anugraha.stays.presentation.screens.new_booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anugraha.stays.domain.usecase.booking.CreateBookingUseCase
import com.anugraha.stays.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewBookingViewModel @Inject constructor(
    private val createBookingUseCase: CreateBookingUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(NewBookingState())
    val state: StateFlow<NewBookingState> = _state.asStateFlow()

    fun handleIntent(intent: NewBookingIntent) {
        when (intent) {
            is NewBookingIntent.GuestNameChanged -> {
                _state.update { it.copy(guestName = intent.name, error = null) }
            }
            is NewBookingIntent.GuestEmailChanged -> {
                _state.update { it.copy(guestEmail = intent.email) }
            }
            is NewBookingIntent.ContactNumberChanged -> {
                _state.update { it.copy(contactNumber = intent.number, error = null) }
            }
            is NewBookingIntent.CheckInDateChanged -> {
                _state.update { it.copy(checkInDate = intent.date) }
            }
            is NewBookingIntent.CheckOutDateChanged -> {
                _state.update { it.copy(checkOutDate = intent.date) }
            }
            is NewBookingIntent.ArrivalTimeChanged -> {
                _state.update { it.copy(arrivalTime = intent.time) }
            }
            is NewBookingIntent.GuestsCountChanged -> {
                _state.update { it.copy(guestsCount = intent.count) }
            }
            is NewBookingIntent.PetToggled -> {
                _state.update { it.copy(hasPet = intent.hasPet) }
            }
            is NewBookingIntent.RoomIdChanged -> {
                _state.update { it.copy(roomId = intent.roomId) }
            }
            is NewBookingIntent.AmountPaidChanged -> {
                _state.update { it.copy(amountPaid = intent.amount) }
            }
            is NewBookingIntent.TransactionIdChanged -> {
                _state.update { it.copy(transactionId = intent.id) }
            }
            is NewBookingIntent.BookingSourceChanged -> {
                _state.update { it.copy(bookingSource = intent.source) }
            }
            NewBookingIntent.CreateBooking -> createBooking()
        }
    }

    private fun createBooking() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = createBookingUseCase(
                guestName = _state.value.guestName,
                guestEmail = _state.value.guestEmail.ifBlank { null },
                contactNumber = _state.value.contactNumber,
                checkInDate = _state.value.checkInDate,
                checkOutDate = _state.value.checkOutDate,
                arrivalTime = _state.value.arrivalTime.ifBlank { null },
                guestsCount = _state.value.guestsCount,
                isPet = _state.value.hasPet,
                roomId = _state.value.roomId,
                amountPaid = _state.value.amountPaid.toDoubleOrNull(),
                transactionId = _state.value.transactionId.ifBlank { null },
                bookingSource = _state.value.bookingSource
            )

            when (result) {
                is NetworkResult.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            error = null
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                NetworkResult.Loading -> {}
            }
        }
    }
}