package com.anugraha.stays.presentation.screens.pending_details

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.presentation.components.AnugrahaCard
import com.anugraha.stays.presentation.components.ErrorScreen
import com.anugraha.stays.presentation.components.LoadingScreen
import com.anugraha.stays.presentation.theme.SecondaryOrange
import com.anugraha.stays.util.DateUtils.toDisplayFormat
import com.anugraha.stays.util.toCurrency
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingDetailsScreen(
    reservationId: Int,
    onNavigateBack: () -> Unit,
    viewModel: PendingDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(reservationId) {
        viewModel.handleIntent(PendingDetailsIntent.LoadReservation(reservationId))
    }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is PendingDetailsEffect.NavigateBack -> onNavigateBack()
                is PendingDetailsEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is PendingDetailsEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (state.showAcceptDialog) {
        ConfirmationDialog(
            title = "Accept Booking",
            message = "Are you sure you want to accept this booking?",
            confirmText = "Yes",
            dismissText = "Back",
            onConfirm = { viewModel.handleIntent(PendingDetailsIntent.ConfirmAccept) },
            onDismiss = { viewModel.handleIntent(PendingDetailsIntent.DismissDialog) }
        )
    }

    if (state.showDeclineDialog) {
        ConfirmationDialog(
            title = "Decline Booking",
            message = "Are you sure you want to decline this booking?",
            confirmText = "Yes",
            dismissText = "Back",
            onConfirm = { viewModel.handleIntent(PendingDetailsIntent.ConfirmDecline) },
            onDismiss = { viewModel.handleIntent(PendingDetailsIntent.DismissDialog) },
            isDestructive = true
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = "Pending Booking Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (state.reservation != null && !state.isProcessing) {
                BottomActionButtons(
                    onAccept = { viewModel.handleIntent(PendingDetailsIntent.ShowAcceptDialog) },
                    onDecline = { viewModel.handleIntent(PendingDetailsIntent.ShowDeclineDialog) }
                )
            }
        }
    ) { paddingValues ->
        when {
            state.isLoading -> LoadingScreen()
            state.isProcessing -> LoadingScreen(message = "Processing...")
            state.error != null && state.reservation == null -> {
                ErrorScreen(
                    message = state.error ?: "Unknown error",
                    onRetry = { viewModel.handleIntent(PendingDetailsIntent.LoadReservation(reservationId)) }
                )
            }
            state.reservation != null -> {
                PendingDetailsContent(
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
private fun PendingDetailsContent(
    reservation: Reservation,
    onCallClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.tertiaryContainer
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PendingActions,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(text = "Pending Approval", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "This booking request needs your review", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
        }

        AnugrahaCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionLabel("Guest Information")
                DetailRow(icon = Icons.Default.Person, label = "Guest Name", value = reservation.primaryGuest.fullName)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    DetailRow(icon = Icons.Default.Phone, label = "Mobile Number", value = reservation.primaryGuest.phone, modifier = Modifier.weight(1f))
                    Button(onClick = { onCallClick(reservation.primaryGuest.phone) }, colors = ButtonDefaults.buttonColors(containerColor = SecondaryOrange)) {
                        Text("CALL")
                    }
                }
                reservation.primaryGuest.email?.let { DetailRow(icon = Icons.Default.Email, label = "Email", value = it) }
                DetailRow(icon = Icons.Default.People, label = "Total Guests", value = "${reservation.adults} Adults${if (reservation.kids > 0) ", ${reservation.kids} Kids" else ""}")
                if (reservation.hasPet) DetailRow(icon = Icons.Default.Pets, label = "Pets", value = "Yes")
            }
        }

        AnugrahaCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionLabel("Booking Details")
                DetailRow(icon = Icons.Default.ConfirmationNumber, label = "Booking Number", value = reservation.reservationNumber)
                DetailRow(icon = Icons.Default.Login, label = "Check-in", value = reservation.checkInDate.toDisplayFormat())
                DetailRow(icon = Icons.Default.Logout, label = "Check-out", value = reservation.checkOutDate.toDisplayFormat())
                reservation.room?.let { DetailRow(icon = Icons.Default.Hotel, label = "Room Type", value = it.title) }
                reservation.estimatedCheckInTime?.let { DetailRow(icon = Icons.Default.AccessTime, label = "Est. Check-in Time", value = it.toString()) }
                DetailRow(icon = Icons.Default.Source, label = "Booking Source", value = reservation.bookingSource.displayName())
            }
        }

        AnugrahaCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionLabel("Payment Information")
                DetailRow(icon = Icons.Default.AttachMoney, label = "Total Amount", value = reservation.totalAmount.toCurrency())
                reservation.paymentStatus?.let { DetailRow(icon = Icons.Default.CreditCard, label = "Payment Status", value = it) }
                reservation.transactionId?.let { DetailRow(icon = Icons.Default.Receipt, label = "Transaction ID", value = it) }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Column {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun BottomActionButtons(onAccept: () -> Unit, onDecline: () -> Unit) {
    Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDecline, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Decline")
            }
            Button(onClick = onAccept, modifier = Modifier.weight(1f)) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Accept")
            }
        }
    }
}

@Composable
private fun ConfirmationDialog(title: String, message: String, confirmText: String, dismissText: String, onConfirm: () -> Unit, onDismiss: () -> Unit, isDestructive: Boolean = false) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = if (isDestructive) Icons.Default.Warning else Icons.Default.CheckCircle, contentDescription = null, tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) },
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        text = { Text(text = message) },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissText) } }
    )
}
