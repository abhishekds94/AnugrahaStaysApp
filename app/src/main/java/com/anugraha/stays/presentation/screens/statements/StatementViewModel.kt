package com.anugraha.stays.presentation.screens.statements

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.anugraha.stays.domain.usecase.statement.ExportStatementPdfUseCase
import com.anugraha.stays.domain.usecase.statement.GenerateStatementUseCase
import com.anugraha.stays.util.BaseViewModel
import com.anugraha.stays.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatementViewModel @Inject constructor(
    private val generateStatementUseCase: GenerateStatementUseCase,
    private val exportStatementPdfUseCase: ExportStatementPdfUseCase,
    @ApplicationContext private val context: Context
) : BaseViewModel<StatementState, StatementIntent, StatementEffect>(StatementState()) {

    override fun handleIntent(intent: StatementIntent) {
        when (intent) {
            is StatementIntent.SelectStartDate -> updateState { it.copy(startDate = intent.date) }
            is StatementIntent.SelectEndDate -> updateState { it.copy(endDate = intent.date) }
            StatementIntent.GenerateStatement -> generateStatement()
            StatementIntent.ExportToPdf -> exportToPdf()
            StatementIntent.DismissError -> updateState { it.copy(error = null, pdfExported = false) }
        }
    }

    private fun generateStatement() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null, hasSearched = false) }

            when (val result = generateStatementUseCase(startDate = currentState.startDate, endDate = currentState.endDate)) {
                is NetworkResult.Success -> {
                    updateState {
                        it.copy(
                            isLoading = false,
                            reservations = result.data?.reservations ?: emptyList(),
                            totalRevenue = result.data?.totalRevenue ?: 0.0,
                            totalBookings = result.data?.totalBookings ?: 0,
                            hasSearched = true
                        )
                    }
                }
                is NetworkResult.Error -> {
                    updateState { it.copy(isLoading = false, error = result.message) }
                    sendEffect(StatementEffect.ShowError(result.message ?: "Failed to generate statement"))
                }
                NetworkResult.Loading -> {}
            }
        }
    }

    private fun exportToPdf() {
        viewModelScope.launch {
            updateState { it.copy(isExportingPdf = true, pdfExported = false) }

            when (val result = exportStatementPdfUseCase(
                context = context,
                startDate = currentState.startDate,
                endDate = currentState.endDate,
                reservations = currentState.reservations,
                totalRevenue = currentState.totalRevenue,
                totalBookings = currentState.totalBookings
            )) {
                is NetworkResult.Success -> {
                    updateState { it.copy(isExportingPdf = false, pdfExported = true) }
                    sendEffect(StatementEffect.PdfExportedSuccessfully)
                }
                is NetworkResult.Error -> {
                    updateState { it.copy(isExportingPdf = false, error = result.message) }
                    sendEffect(StatementEffect.ShowError(result.message ?: "Failed to export PDF"))
                }
                NetworkResult.Loading -> {}
            }
        }
    }
}
