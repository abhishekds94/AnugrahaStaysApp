package com.anugraha.stays.presentation.screens.pending_details

sealed class PendingDetailsIntent {
    data class LoadReservation(val reservationId: Int) : PendingDetailsIntent()
    object ShowAcceptDialog : PendingDetailsIntent()
    object ShowDeclineDialog : PendingDetailsIntent()
    object DismissDialog : PendingDetailsIntent()
    object ConfirmAccept : PendingDetailsIntent()
    object ConfirmDecline : PendingDetailsIntent()
}