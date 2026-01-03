package com.anugraha.stays.presentation.screens.statements

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anugraha.stays.domain.model.BookingSource
import com.anugraha.stays.domain.model.Guest
import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.domain.model.ReservationStatus
import com.anugraha.stays.domain.model.Room
import com.anugraha.stays.presentation.theme.SecondaryOrange
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatementsScreen(
    viewModel: StatementViewModel = hiltViewModel(),
    onNavigateToBooking: (Int) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    val displayFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val dayFormatter = DateTimeFormatter.ofPattern("EEE")
    val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Date pickers
    if (showStartDatePicker) {
        DatePickerDialog(
            selectedDate = state.startDate,
            onDateSelected = { date ->
                viewModel.handleIntent(StatementIntent.SelectStartDate(date))
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false }
        )
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            selectedDate = state.endDate,
            minDate = state.startDate, // Cannot select before start date
            onDateSelected = { date ->
                viewModel.handleIntent(StatementIntent.SelectEndDate(date))
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false }
        )
    }

    // Snackbar for errors
    state.error?.let { error ->
        LaunchedEffect(error) {
            // Error handling
        }
    }

    if (state.pdfExported) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000)
            viewModel.handleIntent(StatementIntent.DismissError)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Statement Generation",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main scrollable content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp) // Top and bottom padding
            ) {
                // Header
                item {
                    Text(
                        text = "Select date range",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Date inputs
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Start Date
                        OutlinedTextField(
                            value = state.startDate.format(dateFormatter),
                            onValueChange = {},
                            label = { Text("Start Date") },
                            placeholder = { Text("DD-MM-YYYY") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Select date"
                                )
                            },
                            readOnly = true,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showStartDatePicker = true },
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        // End Date
                        OutlinedTextField(
                            value = state.endDate.format(dateFormatter),
                            onValueChange = {},
                            label = { Text("End Date") },
                            placeholder = { Text("DD-MM-YYYY") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Select date"
                                )
                            },
                            readOnly = true,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showEndDatePicker = true },
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                // Search Button
                item {
                    Button(
                        onClick = { viewModel.handleIntent(StatementIntent.GenerateStatement) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isLoading
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Searching...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Search")
                        }
                    }
                }

                // Results
                when {
                    state.isLoading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    state.error != null -> {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(
                                    text = state.error ?: "An error occurred",
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    state.hasSearched -> {
                        // Summary Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Title with date range
                                    Text(
                                        text = "Statement for\n${state.startDate.format(DateTimeFormatter.ofPattern("MMM dd"))} - ${state.endDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Revenue and Bookings Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Total Revenue",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                            Text(
                                                text = "₹${"%.2f".format(state.totalRevenue)}",
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Normal,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Total Bookings",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                            Text(
                                                text = state.totalBookings.toString(),
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Normal,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Bookings header
                        item {
                            Text(
                                text = "Bookings (${state.totalBookings})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Generate all dates in range with bookings
                        val datesWithBookings = generateDateRangeWithBookings(
                            startDate = state.startDate,
                            endDate = state.endDate,
                            reservations = state.reservations
                        )

                        // Group by month
                        val groupedByMonth = datesWithBookings.groupBy {
                            YearMonth.from(it.date)
                        }

                        groupedByMonth.forEach { (month, dates) ->
                            // Month Header
                            item {
                                Text(
                                    text = month.format(monthYearFormatter),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            // Date items
                            items(
                                items = dates,
                                key = { "${it.date}-${it.reservations.firstOrNull()?.id ?: 0}" }
                            ) { dateWithBooking ->
                                DayStatementItem(
                                    date = dateWithBooking.date,
                                    reservations = dateWithBooking.reservations,
                                    dayFormatter = dayFormatter,
                                    dateFormatter = dateFormatter,
                                    onBookingClick = onNavigateToBooking
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class DateWithBookings(
    val date: LocalDate,
    val reservations: List<Reservation>
)

fun generateDateRangeWithBookings(
    startDate: LocalDate,
    endDate: LocalDate,
    reservations: List<Reservation>
): List<DateWithBookings> {
    val days = mutableListOf<DateWithBookings>()
    var currentDate = startDate

    while (!currentDate.isAfter(endDate)) {
        // Only match on check-in date, not check-out
        val bookingsForDate = reservations.filter { reservation ->
            reservation.checkInDate == currentDate
        }

        days.add(DateWithBookings(currentDate, bookingsForDate))
        currentDate = currentDate.plusDays(1)
    }

    return days
}

@Composable
private fun DayStatementItem(
    date: LocalDate,
    reservations: List<Reservation>,
    dayFormatter: DateTimeFormatter,
    dateFormatter: DateTimeFormatter,
    onBookingClick: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (reservations.isEmpty())
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Date header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${date.format(dateFormatter)} (${date.format(dayFormatter)})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (reservations.isNotEmpty()) {
                    Text(
                        text = "₹${"%.2f".format(reservations.sumOf { it.totalAmount })}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "No bookings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // Bookings for this day
            if (reservations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider()

                reservations.forEach { reservation ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBookingClick(reservation.id) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Show guest name, not Guest ID
                        Text(
                            text = reservation.primaryGuest.fullName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "View details",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
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
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val date = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                    onDateSelected(date)
                }
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

// Preview
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun StatementsScreenPreview() {
    MaterialTheme {
        StatementsScreenContent(
            state = StatementState(
                startDate = LocalDate.of(2026, 1, 1),
                endDate = LocalDate.of(2026, 1, 31),
                totalRevenue = 12450.0,
                totalBookings = 3,
                reservations = listOf(
                    Reservation(
                        id = 1,
                        reservationNumber = "AS20260103",
                        status = ReservationStatus.APPROVED,
                        checkInDate = LocalDate.of(2026, 1, 3),
                        checkOutDate = LocalDate.of(2026, 1, 4),
                        adults = 2,
                        kids = 1,
                        hasPet = false,
                        totalAmount = 2750.0,
                        primaryGuest = Guest(
                            fullName = "Praveen P",
                            phone = "9739929777",
                            email = "praveen.p@example.com"
                        ),
                        room = Room(
                            id = 2,
                            title = "2 Non A/C Deluxe"
                        ),
                        bookingSource = BookingSource.DIRECT,
                        estimatedCheckInTime = null,
                        transactionId = null,
                        paymentStatus = "Paid",
                        transportService = "No",
                        paymentReference = "T26010214002498886642768"
                    ),
                    Reservation(
                        id = 2,
                        reservationNumber = "AS20260115",
                        status = ReservationStatus.APPROVED,
                        checkInDate = LocalDate.of(2026, 1, 15),
                        checkOutDate = LocalDate.of(2026, 1, 16),
                        adults = 3,
                        kids = 0,
                        hasPet = true,
                        totalAmount = 3500.0,
                        primaryGuest = Guest(
                            fullName = "John Doe",
                            phone = "9876543210",
                            email = "john@example.com"
                        ),
                        room = Room(
                            id = 1,
                            title = "2 A/C Deluxe"
                        ),
                        bookingSource = BookingSource.BOOKING_COM,
                        estimatedCheckInTime = null,
                        transactionId = null,
                        paymentStatus = "Paid",
                        transportService = "Yes",
                        paymentReference = null
                    ),
                    Reservation(
                        id = 3,
                        reservationNumber = "AS20260125",
                        status = ReservationStatus.APPROVED,
                        checkInDate = LocalDate.of(2026, 1, 25),
                        checkOutDate = LocalDate.of(2026, 1, 27),
                        adults = 4,
                        kids = 2,
                        hasPet = false,
                        totalAmount = 6200.0,
                        primaryGuest = Guest(
                            fullName = "Jane Smith",
                            phone = "9123456789",
                            email = "jane@example.com"
                        ),
                        room = Room(
                            id = 2,
                            title = "2 Non A/C Deluxe"
                        ),
                        bookingSource = BookingSource.AIRBNB,
                        estimatedCheckInTime = null,
                        transactionId = null,
                        paymentStatus = "Paid",
                        transportService = "No",
                        paymentReference = "REF123456"
                    )
                ),
                hasSearched = true,
                isLoading = false,
                error = null
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatementsScreenContent(
    state: StatementState
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    val displayFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val dayFormatter = DateTimeFormatter.ofPattern("EEE")
    val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Statement Generation",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
            ) {
                // Header
                item {
                    Text(
                        text = "Select date range",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Date inputs (preview shows static)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = state.startDate.format(dateFormatter),
                            onValueChange = {},
                            label = { Text("Start Date") },
                            readOnly = true,
                            enabled = false,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        OutlinedTextField(
                            value = state.endDate.format(dateFormatter),
                            onValueChange = {},
                            label = { Text("End Date") },
                            readOnly = true,
                            enabled = false,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }

                // Search Button
                item {
                    Button(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Search")
                    }
                }

                if (state.hasSearched) {
                    // Summary Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = "Statement for ${state.startDate.format(DateTimeFormatter.ofPattern("MMM dd"))} - ${state.endDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(48.dp)
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Total Revenue",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            text = "₹${"%.2f".format(state.totalRevenue)}",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Total Bookings",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            text = state.totalBookings.toString(),
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bookings header
                    item {
                        Text(
                            text = "Bookings (${state.totalBookings})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Generate dates
                    val datesWithBookings = generateDateRangeWithBookings(
                        startDate = state.startDate,
                        endDate = state.endDate,
                        reservations = state.reservations
                    )

                    val groupedByMonth = datesWithBookings.groupBy {
                        YearMonth.from(it.date)
                    }

                    groupedByMonth.forEach { (month, dates) ->
                        item {
                            Text(
                                text = month.format(monthYearFormatter),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        items(
                            items = dates,
                            key = { "${it.date}-${it.reservations.firstOrNull()?.id ?: 0}" }
                        ) { dateWithBooking ->
                            DayStatementItem(
                                date = dateWithBooking.date,
                                reservations = dateWithBooking.reservations,
                                dayFormatter = dayFormatter,
                                dateFormatter = dateFormatter,
                                onBookingClick = { }
                            )
                        }
                    }
                }
            }
        }
    }
}