package com.anugraha.stays.presentation.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anugraha.stays.domain.model.AvailabilityStatus
import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.presentation.components.ErrorScreen
import com.anugraha.stays.presentation.components.LoadingScreen
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
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val currentMonth = state.currentMonth

    LaunchedEffect(Unit) {
        viewModel.handleIntent(CalendarIntent.LoadBookings)
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.actionSuccess) {
        if (state.actionSuccess) {
            snackbarHostState.showSnackbar(
                message = "Action completed successfully",
                duration = SnackbarDuration.Short
            )
        }
    }

    LaunchedEffect(state.actionError) {
        state.actionError?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long
            )
        }
    }

    LaunchedEffect(state.syncFailureMessage) {
        state.syncFailureMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long
            )
        }
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        when {
            state.isLoading -> LoadingScreen(message = "Loading calendar data...")
            state.error != null -> ErrorScreen(
                message = state.error ?: "Unknown error",
                onRetry = { viewModel.handleIntent(CalendarIntent.LoadBookings) }
            )
            else -> {
                CalendarContent(
                    reservations = state.reservations,
                    availabilities = state.availabilities,
                    selectedDate = selectedDate,
                    currentMonth = currentMonth,
                    isActionInProgress = state.isActionInProgress,
                    onDateSelected = {
                        selectedDate = it
                        viewModel.handleIntent(CalendarIntent.DateSelected(it))
                    },
                    onMonthChanged = { viewModel.handleIntent(CalendarIntent.LoadMonth(it)) },
                    onBookingClick = onNavigateToBookingDetails,
                    onBlockDate = { viewModel.handleIntent(CalendarIntent.BlockDate(it)) },
                    onOpenDate = { viewModel.handleIntent(CalendarIntent.OpenDate(it)) },
                    onCancelBooking = { reservationId, date ->
                        viewModel.handleIntent(CalendarIntent.CancelBooking(reservationId, date))
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun CalendarContent(
    reservations: List<Reservation>,
    availabilities: List<com.anugraha.stays.domain.model.Availability>,
    selectedDate: LocalDate,
    currentMonth: YearMonth,
    isActionInProgress: Boolean,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChanged: (YearMonth) -> Unit,
    onBookingClick: (Int) -> Unit,
    onBlockDate: (LocalDate) -> Unit,
    onOpenDate: (LocalDate) -> Unit,
    onCancelBooking: (Int, LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Calendar Grid
        CalendarGrid(
            currentMonth = currentMonth,
            selectedDate = selectedDate,
            reservations = reservations,
            availabilities = availabilities,
            onDateSelected = onDateSelected,
            onMonthChanged = onMonthChanged,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Bookings Card
        BookingsCard(
            date = selectedDate,
            reservations = reservations,
            availabilities = availabilities,
            onBookingClick = onBookingClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons - Right after the card, part of scrollable content
        DateActionButton(
            date = selectedDate,
            reservations = reservations,
            availabilities = availabilities,
            isActionInProgress = isActionInProgress,
            onBlockDate = onBlockDate,
            onOpenDate = onOpenDate,
            onCancelBooking = onCancelBooking,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        // Space at bottom to clear bottom navigation
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun CalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    reservations: List<Reservation>,
    availabilities: List<com.anugraha.stays.domain.model.Availability>,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChanged: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Month navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onMonthChanged(currentMonth.minusMonths(1)) }) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous month"
                    )
                }

                Text(
                    text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = { onMonthChanged(currentMonth.plusMonths(1)) }) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next month"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Day headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar days
            val firstDayOfMonth = currentMonth.atDay(1)
            val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
            val daysInMonth = currentMonth.lengthOfMonth()

            Column {
                var dayCounter = 1
                for (week in 0..5) {
                    if (dayCounter > daysInMonth) break

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (dayOfWeek in 0..6) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (week == 0 && dayOfWeek < firstDayOfWeek) {
                                    // Empty cell before month starts
                                } else if (dayCounter <= daysInMonth) {
                                    val date = currentMonth.atDay(dayCounter)
                                    val isSelected = date == selectedDate
                                    val bookingType = getBookingTypeForDate(date, reservations, availabilities)

                                    // Check if date has bookings with specific statuses only
                                    val hasBooking = reservations.any { reservation ->
                                        // Only highlight if status is approved, checkout, or completed
                                        val isValidStatus = when (reservation.status) {
                                            com.anugraha.stays.domain.model.ReservationStatus.APPROVED,
                                            com.anugraha.stays.domain.model.ReservationStatus.CHECKOUT,
                                            com.anugraha.stays.domain.model.ReservationStatus.COMPLETED -> true
                                            else -> false
                                        }

                                        // Check if date falls within reservation AND has valid status
                                        isValidStatus && (
                                                date.isEqual(reservation.checkInDate) ||
                                                        date.isEqual(reservation.checkOutDate) ||
                                                        (date.isAfter(reservation.checkInDate) && date.isBefore(reservation.checkOutDate))
                                                )
                                    }

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
    availabilities: List<com.anugraha.stays.domain.model.Availability>
): BookingType {
    // ✅ PRIORITY 1: Check if blocked by admin FIRST
    val availability = availabilities.find { it.date == date }
    if (availability?.isBlockedByAdmin() == true) {
        return BookingType.BLOCKED
    }

    // ✅ PRIORITY 2: Check for Direct/Website bookings
    val directBooking = reservations.firstOrNull { res ->
        val isValidStatus = when (res.status) {
            com.anugraha.stays.domain.model.ReservationStatus.APPROVED,
            com.anugraha.stays.domain.model.ReservationStatus.CHECKOUT,
            com.anugraha.stays.domain.model.ReservationStatus.COMPLETED -> true
            else -> false
        }

        val isDirect = res.bookingSource == com.anugraha.stays.domain.model.BookingSource.DIRECT ||
                res.bookingSource == com.anugraha.stays.domain.model.BookingSource.WEBSITE

        // Exclude checkout date from highlighting
        isValidStatus && isDirect && (
                date.isEqual(res.checkInDate) ||
                        (date.isAfter(res.checkInDate) && date.isBefore(res.checkOutDate))
                )
    }

    if (directBooking != null) {
        return BookingType.DIRECT_OR_WEBSITE
    }

    // ✅ PRIORITY 3: Check for Airbnb/Booking.com bookings (only if no direct booking or block)
    val externalBooking = reservations.firstOrNull { res ->
        val isValidStatus = when (res.status) {
            com.anugraha.stays.domain.model.ReservationStatus.APPROVED,
            com.anugraha.stays.domain.model.ReservationStatus.CHECKOUT,
            com.anugraha.stays.domain.model.ReservationStatus.COMPLETED -> true
            else -> false
        }

        val isExternal = res.bookingSource == com.anugraha.stays.domain.model.BookingSource.AIRBNB ||
                res.bookingSource == com.anugraha.stays.domain.model.BookingSource.BOOKING_COM

        // Exclude checkout date from highlighting
        isValidStatus && isExternal && (
                date.isEqual(res.checkInDate) ||
                        (date.isAfter(res.checkInDate) && date.isBefore(res.checkOutDate))
                )
    }

    if (externalBooking != null) {
        return when (externalBooking.bookingSource) {
            com.anugraha.stays.domain.model.BookingSource.AIRBNB -> BookingType.AIRBNB
            com.anugraha.stays.domain.model.BookingSource.BOOKING_COM -> BookingType.BOOKING_COM
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
                        BookingType.DIRECT_OR_WEBSITE -> Color(0xFF2196F3).copy(alpha = 0.3f) // Blue
                        BookingType.AIRBNB -> Color(0xFFFF5A5F).copy(alpha = 0.3f)  // Airbnb Red
                        BookingType.BOOKING_COM -> Color(0xFF003580).copy(alpha = 0.3f) // Booking.com Blue
                        BookingType.BLOCKED -> Color(0xFF9E9E9E).copy(alpha = 0.3f)  // Gray
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
    availabilities: List<com.anugraha.stays.domain.model.Availability>,
    onBookingClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val validStatuses = listOf(
        com.anugraha.stays.domain.model.ReservationStatus.APPROVED,
        com.anugraha.stays.domain.model.ReservationStatus.CHECKOUT,
        com.anugraha.stays.domain.model.ReservationStatus.COMPLETED
    )

    // ✅ PRIORITY 1: Check if admin blocked
    val availability = availabilities.find { it.date == date }
    val isBlocked = availability?.isBlockedByAdmin() ?: false

    // ✅ PRIORITY 2: Filter Direct/Website bookings first
    val directCheckIns = reservations.filter {
        it.checkInDate == date &&
                it.status in validStatuses &&
                (it.bookingSource == com.anugraha.stays.domain.model.BookingSource.DIRECT ||
                        it.bookingSource == com.anugraha.stays.domain.model.BookingSource.WEBSITE)
    }

    val directCheckOuts = reservations.filter {
        it.checkOutDate == date &&
                it.status in validStatuses &&
                (it.bookingSource == com.anugraha.stays.domain.model.BookingSource.DIRECT ||
                        it.bookingSource == com.anugraha.stays.domain.model.BookingSource.WEBSITE)
    }

    val directContinued = reservations.filter { reservation ->
        reservation.status in validStatuses &&
                (reservation.bookingSource == com.anugraha.stays.domain.model.BookingSource.DIRECT ||
                        reservation.bookingSource == com.anugraha.stays.domain.model.BookingSource.WEBSITE) &&
                date.isAfter(reservation.checkInDate) &&
                date.isBefore(reservation.checkOutDate)
    }

    // ✅ PRIORITY 3: Only show external bookings if no direct bookings and not blocked
    val hasDirectBookings = directCheckIns.isNotEmpty() ||
            directCheckOuts.isNotEmpty() ||
            directContinued.isNotEmpty()

    val externalCheckIns = if (!hasDirectBookings && !isBlocked) {
        reservations.filter {
            it.checkInDate == date &&
                    it.status in validStatuses &&
                    (it.bookingSource == com.anugraha.stays.domain.model.BookingSource.AIRBNB ||
                            it.bookingSource == com.anugraha.stays.domain.model.BookingSource.BOOKING_COM)
        }
    } else emptyList()

    val externalCheckOuts = if (!hasDirectBookings && !isBlocked) {
        reservations.filter {
            it.checkOutDate == date &&
                    it.status in validStatuses &&
                    (it.bookingSource == com.anugraha.stays.domain.model.BookingSource.AIRBNB ||
                            it.bookingSource == com.anugraha.stays.domain.model.BookingSource.BOOKING_COM)
        }
    } else emptyList()

    val externalContinued = if (!hasDirectBookings && !isBlocked) {
        reservations.filter { reservation ->
            reservation.status in validStatuses &&
                    (reservation.bookingSource == com.anugraha.stays.domain.model.BookingSource.AIRBNB ||
                            reservation.bookingSource == com.anugraha.stays.domain.model.BookingSource.BOOKING_COM) &&
                    date.isAfter(reservation.checkInDate) &&
                    date.isBefore(reservation.checkOutDate)
        }
    } else emptyList()

    // Combine with priority: Direct first, then external
    val checkIns = directCheckIns + externalCheckIns
    val checkOuts = directCheckOuts + externalCheckOuts
    val continuedStays = directContinued + externalContinued

    // Debug logging
    android.util.Log.d("BookingsCard", "========================================")
    android.util.Log.d("BookingsCard", "Date: $date")
    android.util.Log.d("BookingsCard", "Blocked: $isBlocked")
    android.util.Log.d("BookingsCard", "Direct check-ins: ${directCheckIns.size}, check-outs: ${directCheckOuts.size}, continued: ${directContinued.size}")
    android.util.Log.d("BookingsCard", "External check-ins: ${externalCheckIns.size}, check-outs: ${externalCheckOuts.size}, continued: ${externalContinued.size}")
    android.util.Log.d("BookingsCard", "Final - check-ins: ${checkIns.size}, check-outs: ${checkOuts.size}, continued: ${continuedStays.size}")

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Date header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy")),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (isBlocked) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = "Blocked",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "BLOCKED",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bookings content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    // ========== CASE 1: BOTH check-in AND check-out (side-by-side) ==========
                    checkOuts.isNotEmpty() && checkIns.isNotEmpty() -> {
                        android.util.Log.d("BookingsCard", "→ Showing SIDE-BY-SIDE")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Check-out side (LEFT)
                            CompactBookingItem(
                                reservation = checkOuts.first(),
                                isCheckIn = false,
                                onClick = { onBookingClick(checkOuts.first().id) },
                                modifier = Modifier.weight(1f)
                            )

                            // Vertical divider
                            VerticalDivider(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(1.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )

                            // Check-in side (RIGHT)
                            CompactBookingItem(
                                reservation = checkIns.first(),
                                isCheckIn = true,
                                onClick = { onBookingClick(checkIns.first().id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // ========== CASE 2: ONLY check-in OR check-out (full width) ==========
                    checkOuts.isNotEmpty() || checkIns.isNotEmpty() -> {
                        android.util.Log.d("BookingsCard", "→ Showing FULL WIDTH")
                        val reservation = checkOuts.firstOrNull() ?: checkIns.first()
                        val isCheckIn = checkOuts.isEmpty()

                        CompactBookingItem(
                            reservation = reservation,
                            isCheckIn = isCheckIn,
                            onClick = { onBookingClick(reservation.id) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // ========== CASE 3: CONTINUED STAYS (middle days) ==========
                    continuedStays.isNotEmpty() -> {
                        android.util.Log.d("BookingsCard", "→ Showing CONTINUED STAYS")
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            continuedStays.forEach { reservation ->
                                ContinuedStayItem(
                                    reservation = reservation,
                                    onClick = { onBookingClick(reservation.id) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // ========== CASE 4: EMPTY STATE ==========
                    else -> {
                        android.util.Log.d("BookingsCard", "→ Showing EMPTY STATE")
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isBlocked) Icons.Default.Block else Icons.Default.EventBusy,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            Text(
                                text = if (isBlocked) "Date is blocked" else "No bookings found",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactBookingItem(
    reservation: Reservation,
    isCheckIn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Icon(
            imageVector = if (isCheckIn) Icons.Default.Login else Icons.Default.Logout,
            contentDescription = if (isCheckIn) "Check-in" else "Check-out",
            modifier = Modifier.size(24.dp),
            tint = if (isCheckIn)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.error
        )

        // Details
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = if (isCheckIn) "Check-in" else "Check-out",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Text(
                text = reservation.primaryGuest.fullName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            reservation.room?.let { room ->
                Text(
                    text = room.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Chevron indicator
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "View details",
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun ContinuedStayItem(
    reservation: Reservation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon showing it's a continued stay
        Icon(
            imageVector = Icons.Default.MoreHoriz,
            contentDescription = "Continued stay",
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.secondary
        )

        // Details
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Guest name with "- Continued"
            Text(
                text = "${reservation.primaryGuest.fullName} - Continued",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Check-in and check-out dates
            Text(
                text = "Check-in: ${reservation.checkInDate.format(DateTimeFormatter.ofPattern("MMM dd"))} • Check-out: ${reservation.checkOutDate.format(DateTimeFormatter.ofPattern("MMM dd"))}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Room name if available
            reservation.room?.let { room ->
                Text(
                    text = room.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Chevron indicator
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "View details",
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun VerticalDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outline
) {
    Box(
        modifier = modifier.background(color)
    )
}

// ============================================================================
// PREVIEW
// ============================================================================

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CalendarScreenPreview() {
    MaterialTheme {
        CalendarContent(
            reservations = listOf(
                Reservation(
                    id = 1,
                    reservationNumber = "AS20260426",
                    status = com.anugraha.stays.domain.model.ReservationStatus.APPROVED,
                    checkInDate = LocalDate.of(2025, 12, 26),
                    checkOutDate = LocalDate.of(2025, 12, 28),
                    adults = 2,
                    kids = 0,
                    hasPet = false,
                    totalAmount = 5000.0,
                    primaryGuest = com.anugraha.stays.domain.model.Guest(
                        fullName = "K.CHANDRASEKARAN",
                        phone = "9876543210",
                        email = "test@example.com"
                    ),
                    room = com.anugraha.stays.domain.model.Room(
                        id = 1,
                        title = "2 Non A/C Deluxe"
                    ),
                    bookingSource = com.anugraha.stays.domain.model.BookingSource.DIRECT
                )
            ),
            availabilities = emptyList(),
            selectedDate = LocalDate.of(2025, 12, 26),
            currentMonth = YearMonth.of(2025, 12),
            isActionInProgress = false,
            onDateSelected = {},
            onMonthChanged = {},
            onBookingClick = {},
            onBlockDate = {},
            onOpenDate = {},
            onCancelBooking = { _, _ -> }
        )
    }
}

@Composable
private fun DateActionButton(
    date: LocalDate,
    reservations: List<Reservation>,
    availabilities: List<com.anugraha.stays.domain.model.Availability>,
    isActionInProgress: Boolean,
    onBlockDate: (LocalDate) -> Unit,
    onOpenDate: (LocalDate) -> Unit,
    onCancelBooking: (Int, LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    // Only check for bookings with valid statuses
    val validStatuses = listOf(
        com.anugraha.stays.domain.model.ReservationStatus.APPROVED,
        com.anugraha.stays.domain.model.ReservationStatus.CHECKOUT,
        com.anugraha.stays.domain.model.ReservationStatus.COMPLETED,
        com.anugraha.stays.domain.model.ReservationStatus.BLOCKED
    )

    // Check if date has any bookings with valid statuses
    val bookingsOnDate = reservations.filter {
        (it.checkInDate == date || it.checkOutDate == date) &&
                it.status in validStatuses
    }

    // Check if date is blocked
    val availability = availabilities.find { it.date == date }
    val isBlocked = availability?.isBlockedByAdmin() ?: false

    when {
        bookingsOnDate.isNotEmpty() -> {
            // If there are bookings, show single "Cancel Booking" button
            Button(
                onClick = { onCancelBooking(bookingsOnDate.first().id, date) },
                modifier = modifier.height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                enabled = !isActionInProgress
            ) {
                if (isActionInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onError,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Processing...")
                } else {
                    Text(
                        text = "Cancel Booking",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        else -> {
            // If no bookings, show both "Mark as Closed" and "Mark as Open" buttons
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // "Mark as Closed" button (outlined style)
                OutlinedButton(
                    onClick = { onBlockDate(date) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    enabled = !isActionInProgress,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline
                    )
                ) {
                    if (isActionInProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Mark as Closed",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // "Mark as Open" button (filled style)
                Button(
                    onClick = { onOpenDate(date) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    enabled = !isActionInProgress,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isActionInProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Mark as Open",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}