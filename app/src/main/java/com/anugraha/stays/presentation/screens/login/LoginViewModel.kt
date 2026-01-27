package com.anugraha.stays.presentation.screens.login

import androidx.lifecycle.viewModelScope
import com.anugraha.stays.data.local.preferences.UserPreferences
import com.anugraha.stays.domain.usecase.auth.LoginUseCase
import com.anugraha.stays.util.BaseViewModel
import com.anugraha.stays.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val userPreferences: UserPreferences
) : BaseViewModel<LoginState, LoginIntent, LoginEffect>(LoginState()) {

    init {
        loadSavedCredentials()
    }

    override fun handleIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.EmailChanged -> {
                updateState { it.copy(email = intent.email, error = null) }
            }
            is LoginIntent.PasswordChanged -> {
                updateState { it.copy(password = intent.password, error = null) }
            }
            is LoginIntent.RememberMeChanged -> {  // NEW: Handle remember me toggle
                updateState { it.copy(rememberMe = intent.rememberMe) }
            }
            LoginIntent.Login -> login()
        }
    }

    /**
     * Load saved credentials if "Remember Me" was checked previously
     */
    private fun loadSavedCredentials() {
        viewModelScope.launch {
            try {
                val (savedUsername, savedPassword) = userPreferences.getSavedCredentialsSync()

                if (savedUsername != null && savedPassword != null) {
                    updateState {
                        it.copy(
                            email = savedUsername,
                            password = savedPassword,
                            rememberMe = true
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("LoginViewModel", "Error loading saved credentials", e)
            }
        }
    }

    private fun login() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }

            val email = currentState.email.trim()
            val password = currentState.password
            val rememberMe = currentState.rememberMe

            val result = loginUseCase(email = email, password = password)

            when (result) {
                is NetworkResult.Success -> {
                    if (rememberMe) {
                        userPreferences.saveCredentials(
                            username = email,
                            password = password,
                            rememberMe = true
                        )
                    } else {
                        userPreferences.clearRememberMe()
                    }

                    updateState {
                        it.copy(
                            isLoading = false,
                            isLoginSuccessful = true,
                            error = null
                        )
                    }
                    sendEffect(LoginEffect.NavigateToDashboard)
                }
                is NetworkResult.Error -> {
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = result.message,
                            isLoginSuccessful = false
                        )
                    }
                    sendEffect(LoginEffect.ShowError(result.message ?: "Unknown error occurred"))
                }
                NetworkResult.Loading -> {
                }
            }
        }
    }
}