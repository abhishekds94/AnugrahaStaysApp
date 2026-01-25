package com.anugraha.stays.presentation.screens.splash

import androidx.lifecycle.viewModelScope
import com.anugraha.stays.domain.repository.AuthRepository
import com.anugraha.stays.domain.usecase.ical.SyncICalFeedsUseCase
import com.anugraha.stays.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncICalFeedsUseCase: SyncICalFeedsUseCase
) : BaseViewModel<SplashState, SplashIntent, SplashEffect>(SplashState) {

    init {
        handleIntent(SplashIntent.CheckAuth)
    }

    override fun handleIntent(intent: SplashIntent) {
        when (intent) {
            SplashIntent.CheckAuth -> checkAuth()
        }
    }

    private fun checkAuth() {
        viewModelScope.launch {
            delay(2000)
            if (authRepository.isUserLoggedIn()) {
                syncICalFeedsInBackground()
                sendEffect(SplashEffect.NavigateToDashboard)
            } else {
                sendEffect(SplashEffect.NavigateToLogin)
            }
        }
    }

    private fun syncICalFeedsInBackground() {
        viewModelScope.launch {
            syncICalFeedsUseCase()
        }
    }
}
