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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anugraha.stays.presentation.components.AnugrahaTextField
import com.anugraha.stays.presentation.theme.AnugrahaStaysTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewBookingScreen(
    onNavigateBack: () -> Unit
) {
    var guestName by remember { mutableStateOf("") }
    var guestEmail by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }
    var checkInDate by remember { mutableStateOf("") }
    var checkOutDate by remember { mutableStateOf("") }
    var arrivalTime by remember { mutableStateOf("") }
    var numberOfGuests by remember { mutableStateOf(2) }
    var hasPet by remember { mutableStateOf(false) }
    var amountPaid by remember { mutableStateOf("") }
    var transactionId by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf("Direct") }

    Scaffold(
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
            // Guest Information
            SectionHeader("GUEST INFORMATION")

            AnugrahaTextField(
                value = guestName,
                onValueChange = { guestName = it },
                label = "Guest Name",
                placeholder = "Enter guest's full name"
            )

            AnugrahaTextField(
                value = guestEmail,
                onValueChange = { guestEmail = it },
                label = "Guest Email",
                placeholder = "Enter guest's email"
            )

            AnugrahaTextField(
                value = contactNumber,
                onValueChange = { contactNumber = it },
                label = "Contact Number",
                placeholder = "Enter contact number"
            )

            // Booking Details
            SectionHeader("BOOKING DETAILS")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnugrahaTextField(
                    value = checkInDate,
                    onValueChange = { checkInDate = it },
                    label = "Check-in Date",
                    placeholder = "Select date",
                    trailingIcon = {
                        Icon(Icons.Default.CalendarToday, "Calendar")
                    },
                    modifier = Modifier.weight(1f)
                )

                AnugrahaTextField(
                    value = checkOutDate,
                    onValueChange = { checkOutDate = it },
                    label = "Check-out Date",
                    placeholder = "Select date",
                    trailingIcon = {
                        Icon(Icons.Default.CalendarToday, "Calendar")
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            AnugrahaTextField(
                value = arrivalTime,
                onValueChange = { arrivalTime = it },
                label = "Approx. Arrival Time",
                placeholder = "e.g., 3:00 PM"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Number of Guests Counter (simplified for preview)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Number of Guests", style = MaterialTheme.typography.bodyMedium)
                    Text("$numberOfGuests", style = MaterialTheme.typography.titleLarge)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("Pet?", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = hasPet,
                        onCheckedChange = { hasPet = it }
                    )
                }
            }

            // Payment Details
            SectionHeader("PAYMENT DETAILS")

            AnugrahaTextField(
                value = amountPaid,
                onValueChange = { amountPaid = it },
                label = "Amount Paid",
                placeholder = "Enter amount"
            )

            AnugrahaTextField(
                value = transactionId,
                onValueChange = { transactionId = it },
                label = "Transaction ID",
                placeholder = "Enter ID (optional)"
            )

            // Booking Source
            SectionHeader("BOOKING SOURCE")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Direct", "Website", "Airbnb", "Booking.com").forEach { source ->
                    FilterChip(
                        selected = selectedSource == source,
                        onClick = { selectedSource = source },
                        label = { Text(source) }
                    )
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