package com.anugraha.stays.presentation.screens.statements

import com.anugraha.stays.util.ViewEffect

sealed class StatementEffect : ViewEffect {
    object PdfExportedSuccessfully : StatementEffect()
    data class ShowError(val message: String) : StatementEffect()
}
