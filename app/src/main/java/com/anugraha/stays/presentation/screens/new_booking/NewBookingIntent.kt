package com.anugraha.stays.presentation.screens.new_booking

import com.anugraha.stays.util.ViewIntent
import java.time.LocalDate

sealed class NewBookingIntent : ViewIntent {
    data class GuestNameChanged(val name: String) : NewBookingIntent()
    data class GuestEmailChanged(val email: String) : NewBookingIntent()
    data class ContactNumberChanged(val number: String) : NewBookingIntent()
    data class CheckInDateChanged(val date: LocalDate) : NewBookingIntent()
    data class CheckOutDateChanged(val date: LocalDate) : NewBookingIntent()
    data class ArrivalTimeChanged(val time: String) : NewBookingIntent()
    data class GuestsCountChanged(val count: String) : NewBookingIntent()
    data class PetToggled(val hasPet: Boolean) : NewBookingIntent()
    data class NumberOfPetsChanged(val count: Int) : NewBookingIntent()
    data class RoomTypeChanged(val roomType: RoomType) : NewBookingIntent()
    data class NumberOfAcRoomsChanged(val count: Int) : NewBookingIntent()
    data class AmountPaidChanged(val amount: String) : NewBookingIntent()
    data class TransactionIdChanged(val id: String) : NewBookingIntent()
    data class BookingSourceChanged(val source: String) : NewBookingIntent()
    object CreateBooking : NewBookingIntent()
}