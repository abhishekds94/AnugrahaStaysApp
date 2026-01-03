package com.anugraha.stays.presentation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object Reservations : Screen("reservations")
    object Calendar : Screen("calendar")
    object Statements : Screen("statements")
    object NewBooking : Screen("new_booking")
    object BookingDetails : Screen("booking_details/{reservationId}") {
        fun createRoute(reservationId: Int) = "booking_details/$reservationId"
    }
    object PendingDetails : Screen("pending_details/{reservationId}") {
        fun createRoute(reservationId: Int) = "pending_details/$reservationId"
    }
}