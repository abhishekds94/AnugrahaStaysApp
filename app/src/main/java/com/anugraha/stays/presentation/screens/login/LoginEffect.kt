package com.anugraha.stays.presentation.screens.login

import com.anugraha.stays.util.ViewEffect

sealed class LoginEffect : ViewEffect {
    object NavigateToDashboard : LoginEffect()
    data class ShowError(val message: String) : LoginEffect()
}
