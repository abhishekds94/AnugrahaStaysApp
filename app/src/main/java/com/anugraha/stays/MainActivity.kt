package com.anugraha.stays

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.anugraha.stays.domain.usecase.auth.LogoutUseCase
import com.anugraha.stays.presentation.navigation.BottomNavBar
import com.anugraha.stays.presentation.navigation.NavGraph
import com.anugraha.stays.presentation.navigation.Screen
import com.anugraha.stays.presentation.theme.AnugrahaStaysTheme
import com.anugraha.stays.util.BaseViewModel
import com.anugraha.stays.util.BookingNotificationWorkManager
import com.anugraha.stays.util.ViewEffect
import com.anugraha.stays.util.ViewIntent
import com.anugraha.stays.util.ViewState
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var bookingNotificationWorkManager: BookingNotificationWorkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Trigger immediate check when app is opened
        bookingNotificationWorkManager.triggerImmediateCheck()

        setContent {
            AnugrahaStaysTheme {
                MainScreen(
                    navigateTo = intent.getStringExtra("navigate_to"),
                    reservationId = intent.getIntExtra("reservation_id", -1)
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

object MainState : ViewState

sealed class MainIntent : ViewIntent {
    object Logout : MainIntent()
}

sealed class MainEffect : ViewEffect {
    object NavigateToLogin : MainEffect()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val logoutUseCase: LogoutUseCase
) : BaseViewModel<MainState, MainIntent, MainEffect>(MainState) {

    override fun handleIntent(intent: MainIntent) {
        when (intent) {
            MainIntent.Logout -> logout()
        }
    }

    private fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            sendEffect(MainEffect.NavigateToLogin)
        }
    }
}

@Composable
fun MainScreen(
    navigateTo: String? = null,
    reservationId: Int = -1,
    viewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // Handle notification navigation
    LaunchedEffect(navigateTo, reservationId) {
        when (navigateTo) {
            "pending_details" -> {
                if (reservationId != -1) {
                    navController.navigate("${Screen.PendingDetails.route}/$reservationId")
                }
            }
            "reservations" -> {
                navController.navigate(Screen.Reservations.route)
            }
        }
    }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                MainEffect.NavigateToLogin -> {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    val showBottomBar = currentRoute in listOf(
        Screen.Dashboard.route,
        Screen.Reservations.route,
        Screen.Calendar.route,
        Screen.Statements.route,
        Screen.NewBooking.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController)
            }
        }
    ) { paddingValues ->
        NavGraph(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(paddingValues),
            onLogout = { viewModel.handleIntent(MainIntent.Logout) }
        )
    }
}