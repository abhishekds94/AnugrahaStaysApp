package com.anugraha.stays.presentation.screens.reservations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anugraha.stays.domain.model.BookingSource
import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.domain.model.ReservationStatus
import com.anugraha.stays.presentation.components.EmptyState
import com.anugraha.stays.presentation.components.ErrorScreen
import com.anugraha.stays.presentation.components.LoadingScreen
import com.anugraha.stays.util.DateUtils.toDisplayFormat
import kotlinx.coroutines.flow.collectLatest
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationsScreen(
    onNavigateToBookingDetails: (Int) -> Unit,
    viewModel: ReservationsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is ReservationsEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8F9FA))
                    .statusBarsPadding()
            ) {
                Text(
                    text = "All Reservations",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                    color = Color.Black
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(50.dp),
                    color = Color(0xFFE8EAED)
                ) {
                    TextField(
                        value = state.searchQuery,
                        onValueChange = {
                            viewModel.handleIntent(ReservationsIntent.SearchQueryChanged(it))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "Search reservations...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF5F6368)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFF5F6368),
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        viewModel.handleIntent(ReservationsIntent.SearchQueryChanged(""))
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = Color(0xFF5F6368)
                                    )
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFE8EAED),
                            unfocusedContainerColor = Color(0xFFE8EAED),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Color(0xFF003D82),
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        singleLine = true
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8F9FA))
        ) {
            when {
                state.isLoading -> LoadingScreen(message = "Loading all reservations...")
                state.error != null && state.reservations.isEmpty() -> ErrorScreen(
                    message = state.error ?: "Unknown error",
                    onRetry = { viewModel.handleIntent(ReservationsIntent.LoadReservations) }
                )

                state.filteredReservations.isEmpty() -> {
                    EmptyState(
                        message = if (state.searchQuery.isEmpty())
                            "No reservations found"
                        else
                            "No results for \"${state.searchQuery}\""
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 140.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        val sortedMonths = state.getSortedMonths()

                        sortedMonths.forEach { month ->
                            val reservationsInMonth = state.groupedReservations[month] ?: emptyList()
                            val isExpanded = state.expandedMonths.contains(month)

                            item(key = "month_$month") {
                                MonthGroupCard(
                                    month = month,
                                    reservationCount = reservationsInMonth.size,
                                    isExpanded = isExpanded,
                                    reservations = reservationsInMonth,
                                    onToggle = {
                                        viewModel.handleIntent(
                                            ReservationsIntent.ToggleMonthExpansion(month)
                                        )
                                    },
                                    onReservationClick = { reservation ->
                                        if (reservation.bookingSource == BookingSource.DIRECT) {
                                            onNavigateToBookingDetails(reservation.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthGroupCard(
    month: YearMonth,
    reservationCount: Int,
    isExpanded: Boolean,
    reservations: List<Reservation>,
    onToggle: () -> Unit,
    onReservationClick: (Reservation) -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    val monthText = month.format(formatter)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                color = Color(0xFF003D82),
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = if (isExpanded) 0.dp else 20.dp,
                    bottomEnd = if (isExpanded) 0.dp else 20.dp
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = monthText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$reservationCount booking${if (reservationCount != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp
                        )
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5))
                ) {
                    reservations.forEachIndexed { index, reservation ->
                        ReservationItem(
                            reservation = reservation,
                            onClick = { onReservationClick(reservation) },
                            isLast = index == reservations.size - 1
                        )

                        if (index < reservations.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = Color(0xFFE0E0E0),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReservationItem(
    reservation: Reservation,
    onClick: () -> Unit,
    isLast: Boolean,
    modifier: Modifier = Modifier
) {
    val isCancelled = reservation.status == ReservationStatus.ADMIN_CANCELLED
    val isExternal = reservation.bookingSource != BookingSource.DIRECT
    val isClickable = !isExternal

    val content = @Composable {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5))
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .alpha(if (isCancelled) 0.5f else 1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reservation.primaryGuest.fullName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black,
                    textDecoration = if (isCancelled) TextDecoration.LineThrough else null
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = when (reservation.bookingSource) {
                        BookingSource.AIRBNB -> "Airbnb"
                        BookingSource.BOOKING_COM -> "Booking.com"
                        else -> "Website"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5F6368),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (isCancelled) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Cancelled",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFFF9800)
                            )
                            Text(
                                text = "${reservation.checkInDate.toDisplayFormat()} - ${reservation.checkOutDate.toDisplayFormat()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF5F6368),
                                fontSize = 13.sp
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFFF9800)
                            )
                            Text(
                                text = "${reservation.adults + reservation.kids} Guest${if (reservation.adults + reservation.kids != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF5F6368),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = if (isClickable) "View details" else null,
                tint = Color(0xFF9E9E9E),
                modifier = Modifier.size(24.dp)
            )
        }
    }

    if (isClickable) {
        Surface(
            modifier = modifier,
            onClick = onClick,
            color = Color.Transparent
        ) {
            content()
        }
    } else {
        Box(modifier = modifier) {
            content()
        }
    }
}
