package com.anugraha.stays.presentation.screens.statements

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anugraha.stays.domain.model.Reservation
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatementsScreen(
    viewModel: StatementViewModel = hiltViewModel(),
    onNavigateToBooking: (Int) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    val dayFormatter = DateTimeFormatter.ofPattern("EEE")
    val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is StatementEffect.PdfExportedSuccessfully -> {
                    Toast.makeText(context, "Statement exported to PDF", Toast.LENGTH_SHORT).show()
                }
                is StatementEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

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
            minDate = state.startDate,
            onDateSelected = { date ->
                viewModel.handleIntent(StatementIntent.SelectEndDate(date))
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Statement Generation",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
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
                item {
                    Text(
                        text = "Select date range",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = state.startDate.format(dateFormatter),
                            onValueChange = {},
                            label = { Text("Start Date") },
                            trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                            readOnly = true,
                            modifier = Modifier.weight(1f).clickable { showStartDatePicker = true },
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        OutlinedTextField(
                            value = state.endDate.format(dateFormatter),
                            onValueChange = {},
                            label = { Text("End Date") },
                            trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                            readOnly = true,
                            modifier = Modifier.weight(1f).clickable { showEndDatePicker = true },
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }

                item {
                    Button(
                        onClick = { viewModel.handleIntent(StatementIntent.GenerateStatement) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isLoading
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Searching...")
                        } else {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Search")
                        }
                    }
                }

                when {
                    state.isLoading -> {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    state.error != null && state.reservations.isEmpty() -> {
                        item {
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                                Text(text = state.error ?: "An error occurred", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }

                    state.hasSearched -> {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "Statement for\n${state.startDate.format(DateTimeFormatter.ofPattern("MMM dd"))} - ${state.endDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(text = "Total Revenue", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                            Text(text = "₹${"%.2f".format(state.totalRevenue)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(text = "Total Bookings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                            Text(text = state.totalBookings.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text(text = "Bookings (${state.totalBookings})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }

                        val datesWithBookings = generateDateRangeWithBookings(state.startDate, state.endDate, state.reservations)
                        val groupedByMonth = datesWithBookings.groupBy { YearMonth.from(it.date) }

                        groupedByMonth.forEach { (month, dates) ->
                            item {
                                Text(text = month.format(monthYearFormatter), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
                            }
                            items(items = dates, key = { "${it.date}-${it.reservations.firstOrNull()?.id ?: 0}" }) { dateWithBooking ->
                                DayStatementItem(date = dateWithBooking.date, reservations = dateWithBooking.reservations, dayFormatter = dayFormatter, dateFormatter = dateFormatter, onBookingClick = onNavigateToBooking)
                            }
                        }
                    }
                }
            }
        }
    }
}

data class DateWithBookings(val date: LocalDate, val reservations: List<Reservation>)

fun generateDateRangeWithBookings(startDate: LocalDate, endDate: LocalDate, reservations: List<Reservation>): List<DateWithBookings> {
    val days = mutableListOf<DateWithBookings>()
    var currentDate = startDate
    while (!currentDate.isAfter(endDate)) {
        val bookingsForDate = reservations.filter { it.checkInDate == currentDate }
        days.add(DateWithBookings(currentDate, bookingsForDate))
        currentDate = currentDate.plusDays(1)
    }
    return days
}

@Composable
private fun DayStatementItem(date: LocalDate, reservations: List<Reservation>, dayFormatter: DateTimeFormatter, dateFormatter: DateTimeFormatter, onBookingClick: (Int) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (reservations.isEmpty()) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "${date.format(dateFormatter)} (${date.format(dayFormatter)})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (reservations.isNotEmpty()) {
                    Text(text = "₹${"%.2f".format(reservations.sumOf { it.totalAmount })}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                } else {
                    Text(text = "No bookings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
            if (reservations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                reservations.forEach { reservation ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth().clickable { onBookingClick(reservation.id) }.padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = reservation.primaryGuest.fullName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(selectedDate: LocalDate, minDate: LocalDate? = null, onDateSelected: (LocalDate) -> Unit, onDismiss: () -> Unit) {
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
        confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { onDateSelected(LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000))) } }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) { DatePicker(state = datePickerState) }
}
