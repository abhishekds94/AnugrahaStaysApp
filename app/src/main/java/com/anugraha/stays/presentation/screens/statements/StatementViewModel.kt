package com.anugraha.stays.presentation.screens.statements

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anugraha.stays.domain.usecase.statement.GenerateStatementUseCase
import com.anugraha.stays.domain.usecase.statement.ExportStatementPdfUseCase
import com.anugraha.stays.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class StatementViewModel @Inject constructor(
    private val generateStatementUseCase: GenerateStatementUseCase,
    private val exportStatementPdfUseCase: ExportStatementPdfUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(StatementState())
    val state: StateFlow<StatementState> = _state.asStateFlow()

    fun handleIntent(intent: StatementIntent) {
        when (intent) {
            is StatementIntent.SelectStartDate -> selectStartDate(intent.date)
            is StatementIntent.SelectEndDate -> selectEndDate(intent.date)
            is StatementIntent.GenerateStatement -> generateStatement()
            is StatementIntent.ExportToPdf -> exportToPdf()
            is StatementIntent.DismissError -> dismissError()
        }
    }

    private fun selectStartDate(date: LocalDate) {
        _state.update { it.copy(startDate = date) }
    }

    private fun selectEndDate(date: LocalDate) {
        _state.update { it.copy(endDate = date) }
    }

    private fun generateStatement() {
        viewModelScope.launch {
            android.util.Log.d("StatementViewModel", "========== GENERATE STATEMENT ==========")
            android.util.Log.d("StatementViewModel", "Start Date: ${_state.value.startDate}")
            android.util.Log.d("StatementViewModel", "End Date: ${_state.value.endDate}")

            _state.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    hasSearched = false
                )
            }

            when (val result = generateStatementUseCase(
                startDate = _state.value.startDate,
                endDate = _state.value.endDate
            )) {
                is NetworkResult.Success -> {
                    android.util.Log.d("StatementViewModel", "✅ Statement generated successfully")
                    android.util.Log.d("StatementViewModel", "Total Bookings: ${result.data.totalBookings}")
                    android.util.Log.d("StatementViewModel", "Total Revenue: ${result.data.totalRevenue}")

                    _state.update {
                        it.copy(
                            isLoading = false,
                            reservations = result.data.reservations,
                            totalRevenue = result.data.totalRevenue,
                            totalBookings = result.data.totalBookings,
                            hasSearched = true
                        )
                    }
                }
                is NetworkResult.Error -> {
                    android.util.Log.e("StatementViewModel", "❌ Failed to generate statement: ${result.message}")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = result.message,
                            hasSearched = false
                        )
                    }
                }
                NetworkResult.Loading -> {
                    android.util.Log.d("StatementViewModel", "Loading...")
                }
            }
        }
    }

    private fun exportToPdf() {
        viewModelScope.launch {
            android.util.Log.d("StatementViewModel", "========== EXPORT PDF ==========")

            _state.update { it.copy(isExportingPdf = true, pdfExported = false) }

            when (val result = exportStatementPdfUseCase(
                context = context,
                startDate = _state.value.startDate,
                endDate = _state.value.endDate,
                reservations = _state.value.reservations,
                totalRevenue = _state.value.totalRevenue,
                totalBookings = _state.value.totalBookings
            )) {
                is NetworkResult.Success -> {
                    android.util.Log.d("StatementViewModel", "✅ PDF exported successfully: ${result.data}")
                    _state.update {
                        it.copy(
                            isExportingPdf = false,
                            pdfExported = true
                        )
                    }
                }
                is NetworkResult.Error -> {
                    android.util.Log.e("StatementViewModel", "❌ Failed to export PDF: ${result.message}")
                    _state.update {
                        it.copy(
                            isExportingPdf = false,
                            error = result.message
                        )
                    }
                }
                NetworkResult.Loading -> {
                    android.util.Log.d("StatementViewModel", "Exporting PDF...")
                }
            }
        }
    }

    private fun dismissError() {
        _state.update { it.copy(error = null, pdfExported = false) }
    }
}