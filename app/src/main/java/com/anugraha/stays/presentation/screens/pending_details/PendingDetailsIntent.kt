package com.anugraha.stays.presentation.screens.pending_details

import com.anugraha.stays.util.ViewIntent

sealed class PendingDetailsIntent : ViewIntent {
    data class LoadReservation(val reservationId: Int) : PendingDetailsIntent()
    object ShowAcceptDialog : PendingDetailsIntent()
    object ShowDeclineDialog : PendingDetailsIntent()
    object DismissDialog : PendingDetailsIntent()
    object ConfirmAccept : PendingDetailsIntent()
    object ConfirmDecline : PendingDetailsIntent()
}
