package com.anugraha.stays.presentation.screens.login

import com.anugraha.stays.util.ViewState

data class LoginState(
    val email: String = "",
    val password: String = "",
    val rememberMe: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoginSuccessful: Boolean = false
) : ViewState