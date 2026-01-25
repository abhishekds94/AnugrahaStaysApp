package com.anugraha.stays.presentation.screens.new_booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anugraha.stays.presentation.components.AnugrahaTextField
import com.anugraha.stays.presentation.theme.AnugrahaStaysTheme
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewBookingScreen(
    onNavigateBack: () -> Unit,
    viewModel: NewBookingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is NewBookingEffect.NavigateBack -> onNavigateBack()
                is NewBookingEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add New Booking",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SectionHeader("GUEST INFORMATION")

            AnugrahaTextField(
                value = state.guestName,
                onValueChange = { viewModel.handleIntent(NewBookingIntent.GuestNameChanged(it)) },
                label = "Guest Name",
                placeholder = "Enter guest's full name"
            )

            AnugrahaTextField(
                value = state.guestEmail,
                onValueChange = { viewModel.handleIntent(NewBookingIntent.GuestEmailChanged(it)) },
                label = "Guest Email",
                placeholder = "Enter guest's email"
            )

            AnugrahaTextField(
                value = state.contactNumber,
                onValueChange = { viewModel.handleIntent(NewBookingIntent.ContactNumberChanged(it)) },
                label = "Contact Number",
                placeholder = "Enter contact number"
            )

            SectionHeader("BOOKING DETAILS")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnugrahaTextField(
                    value = state.checkInDate.toString(),
                    onValueChange = { },
                    label = "Check-in Date",
                    placeholder = "Select date",
                    trailingIcon = { Icon(Icons.Default.CalendarToday, "Calendar") },
                    modifier = Modifier.weight(1f),
                    enabled = false
                )

                AnugrahaTextField(
                    value = state.checkOutDate.toString(),
                    onValueChange = { },
                    label = "Check-out Date",
                    placeholder = "Select date",
                    trailingIcon = { Icon(Icons.Default.CalendarToday, "Calendar") },
                    modifier = Modifier.weight(1f),
                    enabled = false
                )
            }

            AnugrahaTextField(
                value = state.arrivalTime,
                onValueChange = { viewModel.handleIntent(NewBookingIntent.ArrivalTimeChanged(it)) },
                label = "Approx. Arrival Time",
                placeholder = "e.g., 3:00 PM"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Number of Guests", style = MaterialTheme.typography.bodyMedium)
                    Text("${state.guestsCount}", style = MaterialTheme.typography.titleLarge)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("Pet?", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = state.hasPet,
                        onCheckedChange = { viewModel.handleIntent(NewBookingIntent.PetToggled(it)) }
                    )
                }
            }

            SectionHeader("PAYMENT DETAILS")

            AnugrahaTextField(
                value = state.amountPaid,
                onValueChange = { viewModel.handleIntent(NewBookingIntent.AmountPaidChanged(it)) },
                label = "Amount Paid",
                placeholder = "Enter amount"
            )

            AnugrahaTextField(
                value = state.transactionId,
                onValueChange = { viewModel.handleIntent(NewBookingIntent.TransactionIdChanged(it)) },
                label = "Transaction ID",
                placeholder = "Enter ID (optional)"
            )

            SectionHeader("BOOKING SOURCE")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("DIRECT", "WEBSITE", "AIRBNB", "BOOKING_COM").forEach { source ->
                    FilterChip(
                        selected = state.bookingSource == source,
                        onClick = { viewModel.handleIntent(NewBookingIntent.BookingSourceChanged(source)) },
                        label = { Text(source) }
                    )
                }
            }

            Button(
                onClick = { viewModel.handleIntent(NewBookingIntent.CreateBooking) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading && state.guestName.isNotBlank() && state.contactNumber.isNotBlank()
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Create Booking")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Preview
@Composable
private fun NewBookingPreview() {
    AnugrahaStaysTheme {
        NewBookingScreen(onNavigateBack = {})
    }
}