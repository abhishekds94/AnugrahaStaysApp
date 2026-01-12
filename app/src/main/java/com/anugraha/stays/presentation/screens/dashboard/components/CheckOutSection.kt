package com.anugraha.stays.presentation.screens.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anugraha.stays.domain.model.CheckOut
import com.anugraha.stays.presentation.components.BookingCard
import com.anugraha.stays.presentation.components.EmptyState
import com.anugraha.stays.presentation.theme.AnugrahaStaysTheme

@Composable
fun CheckOutSection(
    checkOuts: List<CheckOut>,
    onBookingClick: (CheckOut) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Today's Check-out",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (checkOuts.isEmpty()) {
            EmptyState(message = "No bookings today")
        } else {
            checkOuts.forEach { checkOut ->
                // UPDATED: Pass reservation object and onClick
                BookingCard(
                    reservation = checkOut.reservation,
                    onClick = { onBookingClick(checkOut) },
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun CheckOutSectionPreview() {
    AnugrahaStaysTheme {
        CheckOutSection(
            checkOuts = emptyList(),
            onBookingClick = {}
        )
    }
}