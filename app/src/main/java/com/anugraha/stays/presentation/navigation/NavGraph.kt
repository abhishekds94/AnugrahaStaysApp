package com.anugraha.stays.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.anugraha.stays.presentation.screens.booking_details.BookingDetailsScreen
import com.anugraha.stays.presentation.screens.calendar.CalendarScreen
import com.anugraha.stays.presentation.screens.dashboard.DashboardScreen
import com.anugraha.stays.presentation.screens.login.LoginScreen
import com.anugraha.stays.presentation.screens.new_booking.NewBookingScreen
import com.anugraha.stays.presentation.screens.reservations.ReservationsScreen
import com.anugraha.stays.presentation.screens.splash.SplashScreen
import com.anugraha.stays.presentation.screens.statements.StatementsScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier,
    onLogout: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToBookingDetails = { reservationId ->
                    navController.navigate(Screen.BookingDetails.createRoute(reservationId))
                },
                onLogout = onLogout
            )
        }

        composable(Screen.Reservations.route) {
            ReservationsScreen(
                onNavigateToBookingDetails = { reservationId ->
                    navController.navigate(Screen.BookingDetails.createRoute(reservationId))
                }
            )
        }

        composable(Screen.Calendar.route) {
            CalendarScreen(
                onNavigateToBookingDetails = { reservationId ->
                    navController.navigate(Screen.BookingDetails.createRoute(reservationId))
                }
            )
        }

        composable(Screen.Statements.route) {
            StatementsScreen(
                onNavigateToBooking = { bookingId ->
                    navController.navigate("booking_details/$bookingId")
                }
            )
        }

        composable(
            route = Screen.BookingDetails.route,
            arguments = listOf(navArgument("reservationId") { type = NavType.IntType })
        ) { backStackEntry ->
            val reservationId = backStackEntry.arguments?.getInt("reservationId") ?: 0
            BookingDetailsScreen(
                reservationId = reservationId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.NewBooking.route) {
            NewBookingScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}