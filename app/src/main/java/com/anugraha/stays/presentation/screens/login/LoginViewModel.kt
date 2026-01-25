package com.anugraha.stays.presentation.screens.login

import androidx.lifecycle.viewModelScope
import com.anugraha.stays.domain.usecase.auth.LoginUseCase
import com.anugraha.stays.util.BaseViewModel
import com.anugraha.stays.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : BaseViewModel<LoginState, LoginIntent, LoginEffect>(LoginState()) {

    override fun handleIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.EmailChanged -> {
                updateState { it.copy(email = intent.email, error = null) }
            }
            is LoginIntent.PasswordChanged -> {
                updateState { it.copy(password = intent.password, error = null) }
            }
            LoginIntent.Login -> login()
        }
    }

    private fun login() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }

            val result = loginUseCase(
                email = currentState.email.trim(),
                password = currentState.password
            )

            when (result) {
                is NetworkResult.Success -> {
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
