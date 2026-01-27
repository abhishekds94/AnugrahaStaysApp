package com.anugraha.stays.presentation.screens.login

import com.anugraha.stays.util.ViewIntent

sealed class LoginIntent : ViewIntent {
    data class EmailChanged(val email: String) : LoginIntent()
    data class PasswordChanged(val password: String) : LoginIntent()
    data class RememberMeChanged(val rememberMe: Boolean) : LoginIntent()
    object Login : LoginIntent()
}