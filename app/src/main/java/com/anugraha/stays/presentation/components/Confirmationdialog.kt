package com.anugraha.stays.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Confirmation dialog for admin actions
 *
 * @param message The confirmation message to display
 * @param onConfirm Callback when YES is clicked
 * @param onDismiss Callback when NO is clicked
 */
@Composable
fun ConfirmationDialog(
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = { }, // Disabled - can't dismiss by clicking outside
        properties = DialogProperties(
            dismissOnBackPress = false, // Can't dismiss with back button
            dismissOnClickOutside = false, // Can't dismiss by clicking outside
            usePlatformDefaultWidth = false // Use custom width
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)), // Black transparent overlay
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Question mark icon in circle
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = Color(0xFF2C2C2C), // Dark gray/black
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Help,
                            contentDescription = "Question",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Title
                    Text(
                        text = "Confirm Action",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C2C2C),
                        textAlign = TextAlign.Center
                    )

                    // Message
                    Text(
                        text = message,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF2C2C2C),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    // Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // NO Button
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC3545) // Red color
                            ),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Text(
                                text = "NO",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // YES Button
                        Button(
                            onClick = onConfirm,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF28A745) // Green color
                            ),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Text(
                                text = "YES",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Predefined confirmation messages for common actions
 */
object ConfirmationMessages {
    const val ACCEPT_BOOKING = "Are you sure you want to ACCEPT this booking?"
    const val DECLINE_BOOKING = "Are you sure you want to DECLINE this booking?"
    const val CANCEL_BOOKING = "Are you sure you want to CANCEL this booking?"
    const val MARK_CLOSED = "Are you sure you want to MARK this day as CLOSE?"
    const val MARK_OPEN = "Are you sure you want to MARK this day as OPEN?"
}

@Preview(showBackground = true)
@Composable
private fun ConfirmationDialogPreviewAccept() {
    MaterialTheme {
        ConfirmationDialog(
            message = ConfirmationMessages.ACCEPT_BOOKING,
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfirmationDialogPreviewDecline() {
    MaterialTheme {
        ConfirmationDialog(
            message = ConfirmationMessages.DECLINE_BOOKING,
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfirmationDialogPreviewCancel() {
    MaterialTheme {
        ConfirmationDialog(
            message = ConfirmationMessages.CANCEL_BOOKING,
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfirmationDialogPreviewClosed() {
    MaterialTheme {
        ConfirmationDialog(
            message = ConfirmationMessages.MARK_CLOSED,
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfirmationDialogPreviewOpen() {
    MaterialTheme {
        ConfirmationDialog(
            message = ConfirmationMessages.MARK_OPEN,
            onConfirm = {},
            onDismiss = {}
        )
    }
}