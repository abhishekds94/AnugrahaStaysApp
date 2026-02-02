package com.anugraha.stays.presentation.screens.booking_details

import android.net.Uri
import com.anugraha.stays.domain.model.Document
import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.util.ViewState

data class BookingDetailsState(
    val reservation: Reservation? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val documents: List<Document> = emptyList(),
    val isUploadingDocument: Boolean = false,
    val showDocumentOptions: Document? = null
) : ViewState