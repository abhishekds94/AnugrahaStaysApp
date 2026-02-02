package com.anugraha.stays.presentation.screens.booking_details

import android.net.Uri
import com.anugraha.stays.domain.model.Document
import com.anugraha.stays.util.ViewIntent

sealed class BookingDetailsIntent : ViewIntent {
    data class LoadBooking(val reservationId: Int) : BookingDetailsIntent()
    data class OpenWhatsApp(val phoneNumber: String) : BookingDetailsIntent()
    object ShowImageSourceDialog : BookingDetailsIntent()
    object DismissImageSourceDialog : BookingDetailsIntent()
    data class UploadDocument(val uri: Uri) : BookingDetailsIntent()
    data class ShowDocumentOptions(val document: Document) : BookingDetailsIntent()
    object DismissDocumentOptions : BookingDetailsIntent()
    data class DeleteDocument(val document: Document) : BookingDetailsIntent()
    data class ViewDocument(val document: Document) : BookingDetailsIntent()
}