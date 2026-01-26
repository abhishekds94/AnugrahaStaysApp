package com.anugraha.stays.presentation.screens.calendar

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anugraha.stays.domain.model.Availability
import com.anugraha.stays.domain.model.BookingSource
import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.domain.model.ReservationStatus
import com.anugraha.stays.presentation.components.ConfirmationDialog
import com.anugraha.stays.presentation.components.ConfirmationMessages
import com.anugraha.stays.presentation.components.ErrorScreen
import com.anugraha.stays.presentation.components.LoadingScreen
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateToBookingDetails: (Int) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Confirmation dialog states
    var showCancelDialog by remember { mutableStateOf(false) }
    var showMarkClosedDialog by remember { mutableStateOf(false) }
    var showMarkOpenDialog by remember { mutableStateOf(false) }

    var selectedBookingId by remember { mutableStateOf<Int?>(null) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is CalendarEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is CalendarEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    // Cancel Booking Confirmation Dialog
    if (showCancelDialog) {
        ConfirmationDialog(
            message = ConfirmationMessages.CANCEL_BOOKING,
            onConfirm = {
                if (selectedBookingId != null && selectedDate != null) {
                    viewModel.handleIntent(
                        CalendarIntent.CancelBooking(selectedBookingId!!, selectedDate!!)
                    )
                }
                showCancelDialog = false
                selectedBookingId = null
                selectedDate = null
            },
            onDismiss = {
                showCancelDialog = false
                selectedBookingId = null
                selectedDate = null
            }
        )
    }

    // Mark as Closed Confirmation Dialog
    if (showMarkClosedDialog) {
        ConfirmationDialog(
            message = ConfirmationMessages.MARK_CLOSED,
            onConfirm = {
                selectedDate?.let { date ->
                    viewModel.handleIntent(CalendarIntent.BlockDate(date))
                }
                showMarkClosedDialog = false
                selectedDate = null
            },
            onDismiss = {
                showMarkClosedDialog = false
                selectedDate = null
            }
        )
    }

    // Mark as Open Confirmation Dialog
    if (showMarkOpenDialog) {
        ConfirmationDialog(
            message = ConfirmationMessages.MARK_OPEN,
            onConfirm = {
                selectedDate?.let { date ->
                    viewModel.handleIntent(CalendarIntent.OpenDate(date))
                }
                showMarkOpenDialog = false
                selectedDate = null
            },
            onDismiss = {
                showMarkOpenDialog = false
                selectedDate = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Availability",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when {
            state.isLoading && state.availabilities.isEmpty() -> LoadingScreen()
            state.error != null && state.availabilities.isEmpty() -> ErrorScreen(
                message = state.error ?: "Unknown error",
                onRetry = { viewModel.handleIntent(CalendarIntent.LoadBookings) }
            )
            else -> {
                CalendarContent(
                    state = state,
                    onIntent = viewModel::handleIntent,
                    onBookingClick = onNavigateToBookingDetails,
                    onCancelBooking = { id, date ->
                        selectedBookingId = id
                        selectedDate = date
                        showCancelDialog = true
                    },
                    onBlockDate = { date ->
                        selectedDate = date
                        showMarkClosedDialog = true
                    },
                    onOpenDate = { date ->
                        selectedDate = date
                        showMarkOpenDialog = true
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun CalendarContent(
    state: CalendarState,
    onIntent: (CalendarIntent) -> Unit,
    onBookingClick: (Int) -> Unit,
    onCancelBooking: (Int, LocalDate) -> Unit,
    onBlockDate: (LocalDate) -> Unit,
    onOpenDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        CalendarGrid(
            currentMonth = state.currentMonth,
            selectedDate = state.selectedDate ?: LocalDate.now(),
            reservations = state.reservations,
            availabilities = state.availabilities,
            onDateSelected = { onIntent(CalendarIntent.DateSelected(it)) },
            onMonthChanged = { onIntent(CalendarIntent.LoadMonth(it)) },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        BookingsCard(
            date = state.selectedDate ?: LocalDate.now(),
            reservations = state.reservations,
            availabilities = state.availabilities,
            onBookingClick = onBookingClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        DateActionButton(
            date = state.selectedDate ?: LocalDate.now(),
            reservations = state.reservations,
            availabilities = state.availabilities,
            isActionInProgress = state.isActionInProgress,
            onBlockDate = onBlockDate,
            onOpenDate = onOpenDate,
            onCancelBooking = onCancelBooking,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun CalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    reservations: List<Reservation>,
    availabilities: List<Availability>,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChanged: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onMonthChanged(currentMonth.minusMonths(1)) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
                }

                Text(
                    text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = { onMonthChanged(currentMonth.plusMonths(1)) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(text = day, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val firstDayOfMonth = currentMonth.atDay(1)
            val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
            val daysInMonth = currentMonth.lengthOfMonth()

            Column {
                var dayCounter = 1
                for (week in 0..5) {
                    if (dayCounter > daysInMonth) break
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        for (dayOfWeek in 0..6) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                                if (week == 0 && dayOfWeek < firstDayOfWeek) {
                                } else if (dayCounter <= daysInMonth) {
                                    val date = currentMonth.atDay(dayCounter)
                                    val isSelected = date == selectedDate
                                    val bookingType = getBookingTypeForDate(date, reservations, availabilities)

                                    CalendarDay(
                                        day = dayCounter,
                                        isSelected = isSelected,
                                        bookingType = bookingType,
                                        onClick = { onDateSelected(date) }
                                    )
                                    dayCounter++
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getBookingTypeForDate(
    date: LocalDate,
    reservations: List<Reservation>,
    availabilities: List<Availability>
): BookingType {
    val availability = availabilities.find { it.date == date }
    if (availability?.isBlockedByAdmin() == true) return BookingType.BLOCKED

    val directBooking = reservations.firstOrNull { res ->
        val isValidStatus = res.status in listOf(ReservationStatus.APPROVED, ReservationStatus.CHECKOUT, ReservationStatus.COMPLETED)
        val isDirect = res.bookingSource in listOf(BookingSource.DIRECT, BookingSource.WEBSITE)
        isValidStatus && isDirect && (date.isEqual(res.checkInDate) || (date.isAfter(res.checkInDate) && date.isBefore(res.checkOutDate)))
    }
    if (directBooking != null) return BookingType.DIRECT_OR_WEBSITE

    val externalBooking = reservations.firstOrNull { res ->
        val isValidStatus = res.status in listOf(ReservationStatus.APPROVED, ReservationStatus.CHECKOUT, ReservationStatus.COMPLETED)
        val isExternal = res.bookingSource in listOf(BookingSource.AIRBNB, BookingSource.BOOKING_COM)
        isValidStatus && isExternal && (date.isEqual(res.checkInDate) || (date.isAfter(res.checkInDate) && date.isBefore(res.checkOutDate)))
    }
    if (externalBooking != null) {
        return when (externalBooking.bookingSource) {
            BookingSource.AIRBNB -> BookingType.AIRBNB
            BookingSource.BOOKING_COM -> BookingType.BOOKING_COM
            else -> BookingType.NONE
        }
    }
    return BookingType.NONE
}

@Composable
private fun CalendarDay(
    day: Int,
    isSelected: Boolean,
    bookingType: BookingType,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> when (bookingType) {
                        BookingType.DIRECT_OR_WEBSITE -> Color(0xFF2196F3).copy(alpha = 0.3f)
                        BookingType.AIRBNB -> Color(0xFFFF5A5F).copy(alpha = 0.3f)
                        BookingType.BOOKING_COM -> Color(0xFF003580).copy(alpha = 0.3f)
                        BookingType.BLOCKED -> Color(0xFF9E9E9E).copy(alpha = 0.3f)
                        BookingType.NONE -> Color.Transparent
                    }
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary
                bookingType == BookingType.BLOCKED -> Color(0xFF9E9E9E)
                else -> MaterialTheme.colorScheme.onSurface
            },
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun BookingsCard(
    date: LocalDate,
    reservations: List<Reservation>,
    availabilities: List<Availability>,
    onBookingClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val validStatuses = listOf(ReservationStatus.APPROVED, ReservationStatus.CHECKOUT, ReservationStatus.COMPLETED)
    val availability = availabilities.find { it.date == date }
    val isBlocked = availability?.isBlockedByAdmin() ?: false

    val directCheckIns = reservations.filter { it.checkInDate == date && it.status in validStatuses && it.bookingSource in listOf(BookingSource.DIRECT, BookingSource.WEBSITE) }
    val directCheckOuts = reservations.filter { it.checkOutDate == date && it.status in validStatuses && it.bookingSource in listOf(BookingSource.DIRECT, BookingSource.WEBSITE) }
    val directContinued = reservations.filter { it.status in validStatuses && it.bookingSource in listOf(BookingSource.DIRECT, BookingSource.WEBSITE) && date.isAfter(it.checkInDate) && date.isBefore(it.checkOutDate) }

    val hasDirectBookings = directCheckIns.isNotEmpty() || directCheckOuts.isNotEmpty() || directContinued.isNotEmpty()

    val externalCheckIns = if (!hasDirectBookings && !isBlocked) {
        reservations.filter { it.checkInDate == date && it.status in validStatuses && it.bookingSource in listOf(BookingSource.AIRBNB, BookingSource.BOOKING_COM) }
    } else emptyList()
    val externalCheckOuts = if (!hasDirectBookings && !isBlocked) {
        reservations.filter { it.checkOutDate == date && it.status in validStatuses && it.bookingSource in listOf(BookingSource.AIRBNB, BookingSource.BOOKING_COM) }
    } else emptyList()
    val externalContinued = if (!hasDirectBookings && !isBlocked) {
        reservations.filter { it.status in validStatuses && it.bookingSource in listOf(BookingSource.AIRBNB, BookingSource.BOOKING_COM) && date.isAfter(it.checkInDate) && date.isBefore(it.checkOutDate) }
    } else emptyList()

    val checkIns = directCheckIns + externalCheckIns
    val checkOuts = directCheckOuts + externalCheckOuts
    val continuedStays = directContinued + externalContinued

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = date.format(DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy")), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (isBlocked) {
                    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Block, contentDescription = "Blocked", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                            Text(text = "BLOCKED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp), contentAlignment = Alignment.Center) {
                when {
                    checkOuts.isNotEmpty() && checkIns.isNotEmpty() -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CompactBookingItem(reservation = checkOuts.first(), isCheckIn = false, onClick = { onBookingClick(checkOuts.first().id) }, modifier = Modifier.weight(1f))
                            Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
                            CompactBookingItem(reservation = checkIns.first(), isCheckIn = true, onClick = { onBookingClick(checkIns.first().id) }, modifier = Modifier.weight(1f))
                        }
                    }
                    checkOuts.isNotEmpty() || checkIns.isNotEmpty() -> {
                        val reservation = checkOuts.firstOrNull() ?: checkIns.first()
                        CompactBookingItem(reservation = reservation, isCheckIn = checkOuts.isEmpty(), onClick = { onBookingClick(reservation.id) }, modifier = Modifier.fillMaxWidth())
                    }
                    continuedStays.isNotEmpty() -> {
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            continuedStays.forEach { ContinuedStayItem(reservation = it, onClick = { onBookingClick(it.id) }, modifier = Modifier.fillMaxWidth()) }
                        }
                    }
                    else -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(if (isBlocked) Icons.Default.Block else Icons.Default.EventBusy, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            Text(if (isBlocked) "Date is blocked" else "No bookings found", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactBookingItem(reservation: Reservation, isCheckIn: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.clickable(onClick = onClick).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (isCheckIn) Icons.Default.Login else Icons.Default.Logout, contentDescription = if (isCheckIn) "Check-in" else "Check-out", modifier = Modifier.size(24.dp), tint = if (isCheckIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(if (isCheckIn) "Check-in" else "Check-out", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text(text = reservation.primaryGuest.fullName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            reservation.room?.let { Text(text = it.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = "View details", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
    }
}

@Composable
private fun ContinuedStayItem(reservation: Reservation, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.clickable(onClick = onClick).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.MoreHoriz, contentDescription = "Continued stay", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.secondary)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "${reservation.primaryGuest.fullName} - Continued", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = "Check-in: ${reservation.checkInDate.format(DateTimeFormatter.ofPattern("MMM dd"))} • Check-out: ${reservation.checkOutDate.format(DateTimeFormatter.ofPattern("MMM dd"))}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            reservation.room?.let { Text(text = it.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = "View details", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
    }
}

@Composable
private fun DateActionButton(
    date: LocalDate,
    reservations: List<Reservation>,
    availabilities: List<Availability>,
    isActionInProgress: Boolean,
    onBlockDate: (LocalDate) -> Unit,
    onOpenDate: (LocalDate) -> Unit,
    onCancelBooking: (Int, LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val validStatuses = listOf(ReservationStatus.APPROVED, ReservationStatus.CHECKOUT, ReservationStatus.COMPLETED, ReservationStatus.BLOCKED)
    val bookingsOnDate = reservations.filter { (it.checkInDate == date || it.checkOutDate == date) && it.status in validStatuses }

    if (bookingsOnDate.isNotEmpty()) {
        Button(
            onClick = { onCancelBooking(bookingsOnDate.first().id, date) },
            modifier = modifier.height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            enabled = !isActionInProgress
        ) {
            if (isActionInProgress) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onError, strokeWidth = 2.dp)
            } else {
                Text(text = "Cancel Booking", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        }
    } else {
        Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { onBlockDate(date) }, modifier = Modifier.weight(1f).height(56.dp), enabled = !isActionInProgress) {
                if (isActionInProgress) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text(text = "Mark as Closed", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            Button(onClick = { onOpenDate(date) }, modifier = Modifier.weight(1f).height(56.dp), enabled = !isActionInProgress) {
                if (isActionInProgress) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                else Text(text = "Mark as Open", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}