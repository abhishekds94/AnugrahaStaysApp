package com.anugraha.stays.presentation.screens.statements

import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.util.ViewState
import java.time.LocalDate

data class StatementState(
    val startDate: LocalDate = LocalDate.now().withDayOfMonth(1),
    val endDate: LocalDate = LocalDate.now(),
    val reservations: List<Reservation> = emptyList(),
    val totalRevenue: Double = 0.0,
    val totalBookings: Int = 0,
    val isLoading: Boolean = false,
    val isExportingPdf: Boolean = false,
    val pdfExported: Boolean = false,
    val hasSearched: Boolean = false,
    val error: String? = null
) : ViewState