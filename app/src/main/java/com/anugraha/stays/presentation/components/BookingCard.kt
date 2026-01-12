package com.anugraha.stays.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anugraha.stays.domain.model.BookingSource
import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.presentation.theme.AnugrahaStaysTheme
import java.time.format.DateTimeFormatter

@Composable
fun BookingCard(
    reservation: Reservation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isExternal = reservation.bookingSource.isExternal()
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

    Card(
        onClick = {
            if (!isExternal) {  // Only allow click for non-external
                onClick()
            }
        },
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isExternal) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isExternal) 0.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row with name and source badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Show booking source name for external, guest name for internal
                    Text(
                        text = if (isExternal) {
                            reservation.bookingSource.getExternalDisplayName()
                        } else {
                            reservation.primaryGuest.fullName
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (!isExternal && reservation.primaryGuest.phone.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = reservation.primaryGuest.phone,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                // Source badge
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when (reservation.bookingSource) {
                        BookingSource.AIRBNB -> Color(0xFFFF5A5F).copy(alpha = 0.1f)
                        BookingSource.BOOKING_COM -> Color(0xFF003580).copy(alpha = 0.1f)
                        else -> MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Text(
                        text = reservation.bookingSource.displayName(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (reservation.bookingSource) {
                            BookingSource.AIRBNB -> Color(0xFFFF5A5F)
                            BookingSource.BOOKING_COM -> Color(0xFF003580)
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dates
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Check-in",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = reservation.checkInDate.format(dateFormatter),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Check-out",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = reservation.checkOutDate.format(dateFormatter),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Show info for external bookings
            if (isExternal) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "External booking - tap disabled",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}