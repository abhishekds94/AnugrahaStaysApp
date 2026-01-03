package com.anugraha.stays.presentation.screens.splash

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import com.anugraha.stays.domain.repository.AuthRepository
import com.anugraha.stays.presentation.theme.AnugrahaStaysTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    suspend fun isUserLoggedIn(): Boolean {
        return authRepository.isUserLoggedIn()
    }
}

@Preview
@Composable
private fun SplashPreview() {
    AnugrahaStaysTheme {
        SplashScreen(
            onNavigateToLogin = {},
            onNavigateToDashboard = {}
        )
    }
}