package com.anugraha.stays.presentation.screens.statements

import com.anugraha.stays.util.ViewIntent
import java.time.LocalDate

sealed class StatementIntent : ViewIntent {
    data class SelectStartDate(val date: LocalDate) : StatementIntent()
    data class SelectEndDate(val date: LocalDate) : StatementIntent()
    object GenerateStatement : StatementIntent()
    object ExportToPdf : StatementIntent()
    object DismissError : StatementIntent()
}
