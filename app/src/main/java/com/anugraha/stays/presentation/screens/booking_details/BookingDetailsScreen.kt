package com.anugraha.stays.presentation.screens.booking_details

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anugraha.stays.presentation.components.ErrorScreen
import com.anugraha.stays.presentation.components.LoadingScreen
import com.anugraha.stays.presentation.theme.AnugrahaStaysTheme
import com.anugraha.stays.presentation.theme.SecondaryOrange
import com.anugraha.stays.util.DateUtils.toDisplayFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailsScreen(
    reservationId: Int,
    onNavigateBack: () -> Unit,
    viewModel: BookingDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(reservationId) {
        viewModel.handleIntent(BookingDetailsIntent.LoadBooking(reservationId))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Booking Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        when {
            state.isLoading -> LoadingScreen()
            state.error != null -> ErrorScreen(
                message = state.error ?: "Unknown error",
                onRetry = {
                    viewModel.handleIntent(BookingDetailsIntent.LoadBooking(reservationId))
                }
            )
            state.reservation != null -> {
                BookingDetailsContent(
                    reservation = state.reservation!!,
                    onCallClick = { phone ->
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun BookingDetailsContent(
    reservation: com.anugraha.stays.domain.model.Reservation,
    onCallClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Single Card with All Information
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // GUEST DETAILS SECTION
                SectionHeader("Guest Details")

                DetailRow(
                    icon = Icons.Default.Person,
                    label = "Guest Name",
                    value = reservation.primaryGuest.fullName
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DetailRow(
                        icon = Icons.Default.Phone,
                        label = "Phone",
                        value = reservation.primaryGuest.phone,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { onCallClick(reservation.primaryGuest.phone) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SecondaryOrange
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CALL")
                    }
                }

                reservation.primaryGuest.email?.let { email ->
                    DetailRow(
                        icon = Icons.Default.Email,
                        label = "Email",
                        value = email
                    )
                } ?: DetailRow(
                    icon = Icons.Default.Email,
                    label = "Email",
                    value = "Not provided",
                    isPlaceholder = true
                )

                // Show address if available from extended guest object
                // Note: This will be "Not provided" for now as it's not in current API response
                DetailRow(
                    icon = Icons.Default.Home,
                    label = "Address",
                    value = "Not provided", // reservation.primaryGuest.address when available
                    isPlaceholder = true
                )

                reservation.estimatedCheckInTime?.let { time ->
                    DetailRow(
                        icon = Icons.Default.AccessTime,
                        label = "Arrival Time",
                        value = time.toString()
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                // ROOM TYPE SECTION
                SectionHeader("Room Type")

                reservation.room?.let { room ->
                    DetailRow(
                        icon = Icons.Default.Hotel,
                        label = "Room",
                        value = room.title
                    )

                    val isAC = room.title.contains("A/C", ignoreCase = true) ||
                            room.title.contains("Air", ignoreCase = true)
                    DetailRow(
                        icon = Icons.Default.AcUnit,
                        label = "Air Conditioned",
                        value = if (isAC) "Yes" else "No"
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                // BOOKING DETAILS SECTION
                SectionHeader("Booking Details")

                DetailRow(
                    icon = Icons.Default.ConfirmationNumber,
                    label = "Reservation Number",
                    value = reservation.reservationNumber
                )

                DetailRow(
                    icon = Icons.Default.Info,
                    label = "Status",
                    value = reservation.status.name.lowercase().replaceFirstChar { it.uppercase() }
                )

                DetailRow(
                    icon = Icons.Default.Login,
                    label = "Check-in",
                    value = reservation.checkInDate.toDisplayFormat()
                )

                DetailRow(
                    icon = Icons.Default.Logout,
                    label = "Check-out",
                    value = reservation.checkOutDate.toDisplayFormat()
                )

                DetailRow(
                    icon = Icons.Default.People,
                    label = "Adults",
                    value = "${reservation.adults}"
                )

                if (reservation.kids > 0) {
                    DetailRow(
                        icon = Icons.Default.ChildCare,
                        label = "Kids",
                        value = "${reservation.kids}"
                    )
                }

                DetailRow(
                    icon = Icons.Default.Pets,
                    label = "Pets",
                    value = if (reservation.hasPet) "Yes" else "No"
                )

                DetailRow(
                    icon = Icons.Default.Source,
                    label = "Booking Source",
                    value = reservation.bookingSource.displayName()
                )

                // Additional requests
                DetailRow(
                    icon = Icons.Default.Description,
                    label = "Additional Requests",
                    value = "None", // reservation.additionalRequests when available
                    isPlaceholder = true
                )

                // Transport service
                DetailRow(
                    icon = Icons.Default.DirectionsCar,
                    label = "Transport Service",
                    value = "No", // reservation.transportService when available
                    isPlaceholder = true
                )

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                // PAYMENT INFORMATION SECTION
                SectionHeader("Payment Information")

                DetailRow(
                    icon = Icons.Default.CurrencyRupee,
                    label = "Total Amount",
                    value = "₹${String.format("%.2f", reservation.totalAmount)}",
                    valueStyle = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                reservation.paymentStatus?.let { status ->
                    DetailRow(
                        icon = Icons.Default.CreditCard,
                        label = "Payment Status",
                        value = status
                    )
                }

                reservation.transactionId?.let { txnId ->
                    DetailRow(
                        icon = Icons.Default.Receipt,
                        label = "Payment Reference",
                        value = txnId
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isPlaceholder: Boolean = false,
    valueStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (isPlaceholder)
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = value,
                style = valueStyle,
                fontWeight = FontWeight.Medium,
                color = if (isPlaceholder)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }
    }
}