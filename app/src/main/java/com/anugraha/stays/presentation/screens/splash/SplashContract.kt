package com.anugraha.stays.presentation.screens.splash

import com.anugraha.stays.util.ViewEffect
import com.anugraha.stays.util.ViewIntent
import com.anugraha.stays.util.ViewState

object SplashState : ViewState

sealed class SplashIntent : ViewIntent {
    object CheckAuth : SplashIntent()
}

sealed class SplashEffect : ViewEffect {
    object NavigateToLogin : SplashEffect()
    object NavigateToDashboard : SplashEffect()
}
