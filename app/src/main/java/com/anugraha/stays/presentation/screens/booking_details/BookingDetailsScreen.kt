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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.domain.model.calculateExpectedAmount
import com.anugraha.stays.domain.model.getPendingBalance
import com.anugraha.stays.presentation.components.ErrorScreen
import com.anugraha.stays.presentation.components.LoadingScreen
import com.anugraha.stays.presentation.theme.SecondaryOrange
import com.anugraha.stays.util.DateUtils.toDisplayFormat
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailsScreen(
    reservationId: Int,
    onNavigateBack: () -> Unit,
    viewModel: BookingDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(reservationId) {
        viewModel.handleIntent(BookingDetailsIntent.LoadBooking(reservationId))
    }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is BookingDetailsEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is BookingDetailsEffect.ShowToast -> {
                    android.widget.Toast.makeText(context, effect.message, android.widget.Toast.LENGTH_SHORT).show()
                }
                is BookingDetailsEffect.OpenWhatsApp -> {
                    try {
                        // Try WhatsApp Business first
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://api.whatsapp.com/send?phone=${effect.phoneNumber}")
                            setPackage("com.whatsapp.w4b")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Fallback to regular WhatsApp
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://api.whatsapp.com/send?phone=${effect.phoneNumber}")
                                setPackage("com.whatsapp")
                            }
                            context.startActivity(intent)
                        } catch (ex: Exception) {
                            // Show error if WhatsApp is not installed
                            android.widget.Toast.makeText(context, "WhatsApp is not installed", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                }
            )
        }
    ) { paddingValues ->
        when {
            state.isLoading -> LoadingScreen()
            state.error != null && state.reservation == null -> ErrorScreen(
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
                    onWhatsAppClick = { phone ->
                        viewModel.handleIntent(BookingDetailsIntent.OpenWhatsApp(phone))
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun BookingDetailsContent(
    reservation: Reservation,
    onCallClick: (String) -> Unit,
    onWhatsAppClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Safe balance calculation with error handling
    val calculatedAmount = try {
        reservation.calculateExpectedAmount()
    } catch (e: Exception) {
        null
    }

    val pendingBalance = try {
        reservation.getPendingBalance()
    } catch (e: Exception) {
        0.0
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                SectionHeader("Guest Details")

                DetailRow(icon = Icons.Default.Person, label = "Guest Name", value = reservation.primaryGuest.fullName)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    DetailRow(icon = Icons.Default.Phone, label = "Phone", value = reservation.primaryGuest.phone, modifier = Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onCallClick(reservation.primaryGuest.phone) },
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryOrange),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CALL")
                        }

                        IconButton(
                            onClick = { onWhatsAppClick(reservation.primaryGuest.phone) },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF25D366))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Whatsapp,
                                contentDescription = "WhatsApp",
                                tint = Color.White
                            )
                        }
                    }
                }

                reservation.primaryGuest.email?.let { DetailRow(icon = Icons.Default.Email, label = "Email", value = it) }
                    ?: DetailRow(icon = Icons.Default.Email, label = "Email", value = "Not provided", isPlaceholder = true)

                DetailRow(icon = Icons.Default.Home, label = "Address", value = "Not provided", isPlaceholder = true)

                reservation.estimatedCheckInTime?.let { DetailRow(icon = Icons.Default.AccessTime, label = "Arrival Time", value = it.toString()) }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                SectionHeader("Room Type")

                reservation.room?.let { room ->
                    DetailRow(icon = Icons.Default.Hotel, label = "Room", value = room.title)
                    val isAC = room.data?.airConditioned ?: (room.title.contains("A/C", ignoreCase = true) || room.title.contains("Air", ignoreCase = true))
                    DetailRow(icon = Icons.Default.AcUnit, label = "Air Conditioned", value = if (isAC) "Yes" else "No")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                SectionHeader("Booking Details")

                DetailRow(icon = Icons.Default.ConfirmationNumber, label = "Reservation Number", value = reservation.reservationNumber)
                DetailRow(icon = Icons.Default.Info, label = "Status", value = reservation.status.name.lowercase().replaceFirstChar { it.uppercase() })
                DetailRow(icon = Icons.Default.Login, label = "Check-in", value = reservation.checkInDate.toDisplayFormat())
                DetailRow(icon = Icons.Default.Logout, label = "Check-out", value = reservation.checkOutDate.toDisplayFormat())
                DetailRow(icon = Icons.Default.People, label = "Adults", value = "${reservation.adults}")
                if (reservation.kids > 0) DetailRow(icon = Icons.Default.ChildCare, label = "Kids", value = "${reservation.kids}")
                DetailRow(icon = Icons.Default.Pets, label = "Pets", value = if (reservation.hasPet) "Yes" else "No")
                DetailRow(icon = Icons.Default.Source, label = "Booking Source", value = reservation.bookingSource.displayName())
                DetailRow(icon = Icons.Default.Description, label = "Additional Requests", value = "None", isPlaceholder = true)
                DetailRow(icon = Icons.Default.DirectionsCar, label = "Transport Service", value = "No", isPlaceholder = true)

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                SectionHeader("Payment Information")

                // Calculated Amount (NEW) - only show if calculation succeeded
                calculatedAmount?.let { amount ->
                    DetailRow(
                        icon = Icons.Default.Calculate,
                        label = "Calculated Amount",
                        value = "₹${String.format("%.2f", amount)}",
                        valueStyle = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    )
                }

                DetailRow(
                    icon = Icons.Default.CurrencyRupee,
                    label = "Total Amount",
                    value = "₹${String.format("%.2f", reservation.totalAmount)}",
                    valueStyle = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                // Show balance if there's a mismatch (NEW) - only if calculation succeeded
                if (pendingBalance != 0.0 && calculatedAmount != null) {
                    DetailRow(
                        icon = if (pendingBalance > 0) Icons.Default.Warning else Icons.Default.Info,
                        label = if (pendingBalance > 0) "Pending Balance" else "Excess Payment",
                        value = "₹${String.format("%.2f", kotlin.math.abs(pendingBalance))}",
                        valueStyle = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (pendingBalance > 0) Color(0xFFD32F2F) else Color(0xFF388E3C)
                        )
                    )
                }

                reservation.paymentStatus?.let { DetailRow(icon = Icons.Default.CreditCard, label = "Payment Status", value = it) }
                reservation.transactionId?.let { DetailRow(icon = Icons.Default.Receipt, label = "Payment Reference", value = it) }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = if (isPlaceholder) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text(text = value, style = valueStyle, fontWeight = FontWeight.Medium, color = if (isPlaceholder) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface)
        }
    }
}