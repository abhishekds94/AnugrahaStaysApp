package com.anugraha.stays.presentation.screens.booking_details

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.anugraha.stays.domain.model.Document
import com.anugraha.stays.domain.repository.ReservationRepository
import com.anugraha.stays.util.BaseViewModel
import com.anugraha.stays.util.FirebaseStorageManager
import com.anugraha.stays.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookingDetailsViewModel @Inject constructor(
    private val reservationRepository: ReservationRepository,
    private val firebaseStorageManager: FirebaseStorageManager
) : BaseViewModel<BookingDetailsState, BookingDetailsIntent, BookingDetailsEffect>(BookingDetailsState()) {

    override fun handleIntent(intent: BookingDetailsIntent) {
        when (intent) {
            is BookingDetailsIntent.LoadBooking -> loadBooking(intent.reservationId)
            is BookingDetailsIntent.OpenWhatsApp -> openWhatsApp(intent.phoneNumber)
            BookingDetailsIntent.ShowImageSourceDialog -> sendEffect(BookingDetailsEffect.ShowImageSourceDialog)
            BookingDetailsIntent.DismissImageSourceDialog -> { /* No state change needed */ }
            is BookingDetailsIntent.UploadDocument -> uploadDocument(intent.uri)
            is BookingDetailsIntent.ShowDocumentOptions -> updateState { it.copy(showDocumentOptions = intent.document) }
            BookingDetailsIntent.DismissDocumentOptions -> updateState { it.copy(showDocumentOptions = null) }
            is BookingDetailsIntent.DeleteDocument -> deleteDocument(intent.document)
            is BookingDetailsIntent.ViewDocument -> viewDocument(intent.document)
        }
    }

    private fun openWhatsApp(phoneNumber: String) {
        sendEffect(BookingDetailsEffect.OpenWhatsApp(phoneNumber))
    }

    private fun loadBooking(reservationId: Int) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }

            when (val result = reservationRepository.getReservationById(reservationId)) {
                is NetworkResult.Success -> {
                    updateState {
                        it.copy(
                            reservation = result.data,
                            isLoading = false
                        )
                    }
                    // Load documents for this reservation
                    loadDocuments(reservationId)
                }
                is NetworkResult.Error -> {
                    updateState {
                        it.copy(
                            error = result.message,
                            isLoading = false
                        )
                    }
                    sendEffect(BookingDetailsEffect.ShowError(result.message ?: "Failed to load details"))
                }
                NetworkResult.Loading -> {}
            }
        }
    }

    private fun loadDocuments(reservationId: Int) {
        // TODO: Load documents from backend/Firebase
        // For now, using local state only
        // In production, you'd fetch from Firestore or your backend
    }

    private fun uploadDocument(uri: Uri) {
        val checkInDate = currentState.reservation?.checkInDate ?: return
        val guestName = currentState.reservation?.primaryGuest?.fullName ?: return

        viewModelScope.launch {
            updateState { it.copy(isUploadingDocument = true) }

            val result = firebaseStorageManager.uploadDocument(uri, checkInDate, guestName)

            result.onSuccess { downloadUrl ->
                // Create document object
                val document = Document(
                    id = System.currentTimeMillis().toString(),
                    name = "ID_Front.jpg",
                    url = downloadUrl,
                    uploadedAt = System.currentTimeMillis()
                )

                // Add to state
                updateState {
                    it.copy(
                        documents = it.documents + document,
                        isUploadingDocument = false
                    )
                }

                sendEffect(BookingDetailsEffect.ShowToast("Document uploaded successfully"))

                // TODO: Save document reference to backend
            }.onFailure { error ->
                updateState { it.copy(isUploadingDocument = false) }
                sendEffect(BookingDetailsEffect.ShowError("Failed to upload document: ${error.message}"))
            }
        }
    }

    private fun deleteDocument(document: Document) {
        viewModelScope.launch {
            updateState { it.copy(showDocumentOptions = null) }

            val result = firebaseStorageManager.deleteDocument(document.url)

            result.onSuccess {
                // Remove from state
                updateState {
                    it.copy(documents = it.documents.filter { doc -> doc.id != document.id })
                }

                sendEffect(BookingDetailsEffect.ShowToast("Document deleted"))

                // TODO: Delete document reference from backend
            }.onFailure { error ->
                sendEffect(BookingDetailsEffect.ShowError("Failed to delete document: ${error.message}"))
            }
        }
    }

    private fun viewDocument(document: Document) {
        updateState { it.copy(showDocumentOptions = null) }
        sendEffect(BookingDetailsEffect.OpenImageViewer(document.url))
    }
}