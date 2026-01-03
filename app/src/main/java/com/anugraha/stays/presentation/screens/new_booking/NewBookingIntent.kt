package com.anugraha.stays.presentation.screens.new_booking

sealed class NewBookingIntent {
    data class GuestNameChanged(val name: String) : NewBookingIntent()
    data class GuestEmailChanged(val email: String) : NewBookingIntent()
    data class ContactNumberChanged(val number: String) : NewBookingIntent()
    data class CheckInDateChanged(val date: String) : NewBookingIntent()
    data class CheckOutDateChanged(val date: String) : NewBookingIntent()
    data class ArrivalTimeChanged(val time: String) : NewBookingIntent()
    data class GuestsCountChanged(val count: Int) : NewBookingIntent()
    data class PetToggled(val hasPet: Boolean) : NewBookingIntent()
    data class RoomIdChanged(val roomId: Int) : NewBookingIntent()
    data class AmountPaidChanged(val amount: String) : NewBookingIntent()
    data class TransactionIdChanged(val id: String) : NewBookingIntent()
    data class BookingSourceChanged(val source: String) : NewBookingIntent()
    object CreateBooking : NewBookingIntent()
}