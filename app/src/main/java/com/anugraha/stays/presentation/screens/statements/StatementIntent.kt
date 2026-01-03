package com.anugraha.stays.presentation.screens.statements

import java.time.LocalDate

sealed class StatementIntent {
    data class SelectStartDate(val date: LocalDate) : StatementIntent()
    data class SelectEndDate(val date: LocalDate) : StatementIntent()
    object GenerateStatement : StatementIntent()
    object ExportToPdf : StatementIntent()
    object DismissError : StatementIntent()
}