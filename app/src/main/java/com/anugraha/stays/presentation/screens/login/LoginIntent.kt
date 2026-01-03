package com.anugraha.stays.presentation.screens.login

sealed class LoginIntent {
    data class EmailChanged(val email: String) : LoginIntent()
    data class PasswordChanged(val password: String) : LoginIntent()
    object Login : LoginIntent()
}