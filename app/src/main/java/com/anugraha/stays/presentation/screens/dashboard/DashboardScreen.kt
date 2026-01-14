package com.anugraha.stays.presentation.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anugraha.stays.presentation.components.EmptyState
import com.anugraha.stays.presentation.components.ErrorScreen
import com.anugraha.stays.presentation.components.LoadingScreen
import com.anugraha.stays.presentation.screens.dashboard.components.CheckInSection
import com.anugraha.stays.presentation.screens.dashboard.components.CheckOutSection
import com.anugraha.stays.presentation.screens.dashboard.components.PendingReservationsSection
import com.anugraha.stays.presentation.screens.dashboard.components.WeekBookingsSection
import com.anugraha.stays.presentation.theme.AnugrahaStaysTheme
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToBookingDetails: (Int) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard", fontWeight = FontWeight.Bold) },
                actions = {
                    // ADD THIS SYNC BUTTON
                    IconButton(onClick = {
                        viewModel.handleIntent(DashboardIntent.ForceResync)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Force re-sync"
                        )
                    }

                    // Your existing actions...
                    IconButton(onClick = { /* notifications */ }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                    IconButton(onClick = { /* profile */ }) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            state.isLoading && !state.isRefreshing -> {
                LoadingScreen()
            }

            state.error != null && state.todayCheckIns.isEmpty() -> {
                ErrorScreen(
                    message = state.error ?: "Unknown error",
                    onRetry = { viewModel.handleIntent(DashboardIntent.LoadData) }
                )
            }

            else -> {
                DashboardContent(
                    state = state,
                    onNavigateToBookingDetails = onNavigateToBookingDetails,
                    onNavigateToPendingDetails = onNavigateToBookingDetails, // ADDED
                    onRefresh = { viewModel.handleIntent(DashboardIntent.RefreshData) },
                    onAcceptReservation = { id ->
                        viewModel.handleIntent(DashboardIntent.AcceptReservation(id))
                    },
                    onDeclineReservation = { id ->
                        viewModel.handleIntent(DashboardIntent.DeclineReservation(id))
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun DashboardContent(
    state: DashboardState,
    onNavigateToBookingDetails: (Int) -> Unit,
    onNavigateToPendingDetails: (Int) -> Unit, // ADDED
    onRefresh: () -> Unit,
    onAcceptReservation: (Int) -> Unit,
    onDeclineReservation: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    SwipeRefresh(
        state = rememberSwipeRefreshState(state.isRefreshing),
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 80.dp  // Add extra padding at bottom for navbar
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                CheckInSection(
                    checkIns = state.todayCheckIns,
                    onBookingClick = { onNavigateToBookingDetails(it.reservation.id) },
                    isLoading = state.isLoadingCheckIns
                )
            }

            item {
                CheckOutSection(
                    checkOuts = state.todayCheckOuts,
                    onBookingClick = { onNavigateToBookingDetails(it.reservation.id) },
                    isLoading = state.isLoadingCheckOuts
                )
            }

            item {
                WeekBookingsSection(
                    weekBookings = state.weekBookings,
                    onBookingClick = { onNavigateToBookingDetails(it.reservation.id) },
                    isLoading = state.isLoadingWeekBookings
                )
            }

            item {
                PendingReservationsSection(
                    pendingReservations = state.pendingReservations,
                    onDetailsClick = { onNavigateToPendingDetails(it.id) }, // UPDATED: Navigate to pending details
                    onAccept = onAcceptReservation,
                    onDecline = onDeclineReservation,
                    isLoading = state.isLoadingPendingReservations
                )
            }
        }
    }
}

@Preview
@Composable
private fun DashboardPreview() {
    AnugrahaStaysTheme {
        DashboardContent(
            state = DashboardState(),
            onNavigateToBookingDetails = {},
            onNavigateToPendingDetails = {},
            onRefresh = {},
            onAcceptReservation = {},
            onDeclineReservation = {}
        )
    }
}