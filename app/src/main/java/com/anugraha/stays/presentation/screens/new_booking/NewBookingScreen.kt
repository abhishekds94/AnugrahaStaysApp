package com.anugraha.stays.presentation.screens.new_booking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anugraha.stays.presentation.components.AnugrahaTextField
import com.anugraha.stays.presentation.theme.AnugrahaStaysTheme
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewBookingScreen(
    onNavigateBack: () -> Unit,
    viewModel: NewBookingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCheckInDatePicker by remember { mutableStateOf(false) }
    var showCheckOutDatePicker by remember { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is NewBookingEffect.NavigateBack -> onNavigateBack()
                is NewBookingEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    // Check-in Date Picker Dialog
    if (showCheckInDatePicker) {
        DatePickerDialog(
            title = "Select Check-in Date",
            selectedDate = state.checkInDate,
            onDateSelected = { date ->
                viewModel.handleIntent(NewBookingIntent.CheckInDateChanged(date))
                showCheckInDatePicker = false
            },
            onDismiss = { showCheckInDatePicker = false }
        )
    }

    // Check-out Date Picker Dialog
    if (showCheckOutDatePicker) {
        DatePickerDialog(
            title = "Select Check-out Date",
            selectedDate = state.checkOutDate,
            minDate = state.checkInDate,
            onDateSelected = { date ->
                viewModel.handleIntent(NewBookingIntent.CheckOutDateChanged(date))
                showCheckOutDatePicker = false
            },
            onDismiss = { showCheckOutDatePicker = false }
        )
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
                .padding(16.dp)
                .padding(bottom = 80.dp),
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
                placeholder = "Enter guest's email",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            AnugrahaTextField(
                value = state.contactNumber,
                onValueChange = { viewModel.handleIntent(NewBookingIntent.ContactNumberChanged(it)) },
                label = "Contact Number",
                placeholder = "Enter contact number",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            SectionHeader("BOOKING DETAILS")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Check-in Date Field
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Check-in Date",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = state.checkInDate.format(dateFormatter),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Calendar")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCheckInDatePicker = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                // Check-out Date Field
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Check-out Date",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = state.checkOutDate.format(dateFormatter),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Calendar")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCheckOutDatePicker = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            AnugrahaTextField(
                value = state.arrivalTime,
                onValueChange = { viewModel.handleIntent(NewBookingIntent.ArrivalTimeChanged(it)) },
                label = "Approx. Arrival Time",
                placeholder = "e.g., 3:00 PM"
            )

            // Number of Guests - Fixed Number Input
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Number of Guests",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = if (state.guestsCount == 0) "" else state.guestsCount.toString(),
                    onValueChange = { input ->
                        // Allow empty string or valid numbers
                        if (input.isEmpty()) {
                            viewModel.handleIntent(NewBookingIntent.GuestsCountChanged("0"))
                        } else {
                            viewModel.handleIntent(NewBookingIntent.GuestsCountChanged(input))
                        }
                    },
                    placeholder = { Text("Enter number of guests") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Room Type Dropdown
            var expandedRoomType by remember { mutableStateOf(false) }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Room Type",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = expandedRoomType,
                    onExpandedChange = { expandedRoomType = it }
                ) {
                    OutlinedTextField(
                        value = if (state.roomType == RoomType.AC) "A/C Room" else "Non A/C Room",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedRoomType,
                        onDismissRequest = { expandedRoomType = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Non A/C Room") },
                            onClick = {
                                viewModel.handleIntent(NewBookingIntent.RoomTypeChanged(RoomType.NON_AC))
                                expandedRoomType = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("A/C Room") },
                            onClick = {
                                viewModel.handleIntent(NewBookingIntent.RoomTypeChanged(RoomType.AC))
                                expandedRoomType = false
                            }
                        )
                    }
                }
            }

            // Number of AC Rooms (only show if AC selected)
            if (state.roomType == RoomType.AC) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Number of A/C Rooms",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        RadioOption(
                            text = "One Room",
                            selected = state.numberOfAcRooms == 1,
                            onClick = { viewModel.handleIntent(NewBookingIntent.NumberOfAcRoomsChanged(1)) },
                            modifier = Modifier.weight(1f)
                        )

                        RadioOption(
                            text = "Two Rooms",
                            selected = state.numberOfAcRooms == 2,
                            onClick = { viewModel.handleIntent(NewBookingIntent.NumberOfAcRoomsChanged(2)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Pet - Yes/No Radio
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Pet?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RadioOption(
                        text = "No",
                        selected = !state.hasPet,
                        onClick = { viewModel.handleIntent(NewBookingIntent.PetToggled(false)) },
                        modifier = Modifier.weight(1f)
                    )

                    RadioOption(
                        text = "Yes",
                        selected = state.hasPet,
                        onClick = { viewModel.handleIntent(NewBookingIntent.PetToggled(true)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Number of Pets (only show if hasPet is true)
            if (state.hasPet) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Number of Pets",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        RadioOption(
                            text = "One Pet",
                            selected = state.numberOfPets == 1,
                            onClick = { viewModel.handleIntent(NewBookingIntent.NumberOfPetsChanged(1)) },
                            modifier = Modifier.weight(1f)
                        )

                        RadioOption(
                            text = "Two Pets",
                            selected = state.numberOfPets == 2,
                            onClick = { viewModel.handleIntent(NewBookingIntent.NumberOfPetsChanged(2)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            SectionHeader("PAYMENT DETAILS")

            // Calculated Amount Hint
            val calculatedAmount = state.calculateTotalAmount()
            if (calculatedAmount > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Calculated Total Amount:",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "₹${"%.2f".format(calculatedAmount)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            AnugrahaTextField(
                value = state.amountPaid,
                onValueChange = { viewModel.handleIntent(NewBookingIntent.AmountPaidChanged(it)) },
                label = "Amount Paid",
                placeholder = "Enter amount",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
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
                listOf("DIRECT", "AIRBNB", "BOOKING_COM").forEach { source ->
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
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
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

@Composable
private fun RadioOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (selected) {
            null
        } else {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline
            )
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RadioButton(
                selected = selected,
                onClick = null
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    title: String,
    selectedDate: LocalDate,
    minDate: LocalDate? = null,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.toEpochDay() * 24 * 60 * 60 * 1000,
        selectableDates = if (minDate != null) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = LocalDate.ofEpochDay(utcTimeMillis / (24 * 60 * 60 * 1000))
                    return !date.isBefore(minDate)
                }
            }
        } else {
            object : SelectableDates {}
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let {
                        onDateSelected(LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000)))
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp)
            )
            DatePicker(state = datePickerState)
        }
    }
}

@Preview
@Composable
private fun NewBookingPreview() {
    AnugrahaStaysTheme {
        NewBookingScreen(onNavigateBack = {})
    }
}