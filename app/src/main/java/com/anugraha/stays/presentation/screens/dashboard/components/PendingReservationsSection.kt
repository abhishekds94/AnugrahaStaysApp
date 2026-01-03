package com.anugraha.stays.presentation.screens.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.presentation.components.AnugrahaActionButton
import com.anugraha.stays.presentation.components.AnugrahaCard
import com.anugraha.stays.presentation.components.EmptyState
import com.anugraha.stays.presentation.theme.PendingCardBackground
import com.anugraha.stays.presentation.theme.SecondaryOrange
import com.anugraha.stays.util.DateUtils.toDisplayFormat

@Composable
fun PendingReservationsSection(
    pendingReservations: List<Reservation>,
    onDetailsClick: (Reservation) -> Unit,
    onAccept: (Int) -> Unit,
    onDecline: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Pending Reservations",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (pendingReservations.isEmpty()) {
            EmptyState(message = "No pending reservations")
        } else {
            pendingReservations.forEach { reservation ->
                PendingReservationCard(
                    reservation = reservation,
                    onDetailsClick = { onDetailsClick(reservation) },
                    onAccept = { onAccept(reservation.id) },
                    onDecline = { onDecline(reservation.id) },
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun PendingReservationCard(
    reservation: Reservation,
    onDetailsClick: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnugrahaCard(
        modifier = modifier,
        backgroundColor = PendingCardBackground
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reservation.primaryGuest.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = reservation.primaryGuest.phone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${reservation.checkInDate.toDisplayFormat()} - ${reservation.checkOutDate.toDisplayFormat()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnugrahaActionButton(
                    text = "Details",
                    onClick = onDetailsClick,
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                AnugrahaActionButton(
                    text = "Accept",
                    onClick = onAccept,
                    backgroundColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                AnugrahaActionButton(
                    text = "Decline",
                    onClick = onDecline,
                    backgroundColor = SecondaryOrange,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}