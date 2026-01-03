package com.anugraha.stays.presentation.screens.pending_details

import com.anugraha.stays.domain.model.Reservation

data class PendingDetailsState(
    val reservation: Reservation? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAcceptDialog: Boolean = false,
    val showDeclineDialog: Boolean = false,
    val isProcessing: Boolean = false,
    val actionSuccess: Boolean = false
)