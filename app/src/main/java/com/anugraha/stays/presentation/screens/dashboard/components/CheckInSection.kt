package com.anugraha.stays.presentation.screens.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anugraha.stays.domain.model.CheckIn
import com.anugraha.stays.presentation.components.BookingCard
import com.anugraha.stays.presentation.components.EmptyState
import com.anugraha.stays.presentation.theme.AnugrahaStaysTheme
import com.anugraha.stays.util.DateUtils.toDisplayFormat

@Composable
fun CheckInSection(
    checkIns: List<CheckIn>,
    onBookingClick: (CheckIn) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Today's Check-ins",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (checkIns.isEmpty()) {
            EmptyState(message = "No bookings today")
        } else {
            checkIns.forEach { checkIn ->
                val additionalInfo = buildString {
                    append("Check-in: ${checkIn.reservation.checkInDate.toDisplayFormat()}")
                    checkIn.checkInTime?.let {
                        append(" at $it")
                    }
                }

                BookingCard(
                    guestName = checkIn.reservation.primaryGuest.fullName,
                    phoneNumber = checkIn.reservation.primaryGuest.phone,
                    additionalInfo = additionalInfo,
                    onClick = { onBookingClick(checkIn) },
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun CheckInSectionPreview() {
    AnugrahaStaysTheme {
        CheckInSection(
            checkIns = emptyList(),
            onBookingClick = {}
        )
    }
}