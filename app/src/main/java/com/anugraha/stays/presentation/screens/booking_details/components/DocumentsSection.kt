package com.anugraha.stays.presentation.screens.booking_details.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anugraha.stays.domain.model.Document
import com.anugraha.stays.presentation.components.DocumentOptionsDialog
import com.anugraha.stays.presentation.components.ImageSourceDialog
import java.io.File

/**
 * Documents section showing uploaded documents and upload placeholder
 */
@Composable
fun DocumentsSection(
    documents: List<Document>,
    isUploading: Boolean,
    showImageSourceDialog: Boolean,
    showDocumentOptions: Document?,
    onUploadClick: () -> Unit,
    onDocumentClick: (Document) -> Unit,
    onGallerySelected: (Uri) -> Unit,
    onCameraSelected: (Uri) -> Unit,
    onViewDocument: (Document) -> Unit,
    onDeleteDocument: (Document) -> Unit,
    onDismissImageSource: () -> Unit,
    onDismissDocumentOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onGallerySelected(it) }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val uri = cameraImageUri
            if (uri != null) {
                onCameraSelected(uri)
            }
        }
    }

    // Create temp file for camera
    fun createTempImageFile(): Uri {
        val tempFile = File.createTempFile(
            "camera_${System.currentTimeMillis()}",
            ".jpg",
            context.cacheDir
        ).apply {
            createNewFile()
            deleteOnExit()
        }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            tempFile
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "DOCUMENTS",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Documents grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Uploaded documents
            documents.forEach { document ->
                DocumentCard(
                    document = document,
                    onClick = { onDocumentClick(document) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Upload placeholder or loading
            if (isUploading) {
                UploadingCard(modifier = Modifier.weight(1f))
            } else if (documents.size < 4) { // Limit to 4 documents
                UploadPlaceholder(
                    onClick = onUploadClick,
                    modifier = Modifier.weight(1f)
                )
            }

            // Fill remaining space
            repeat(maxOf(0, 2 - documents.size - if (isUploading) 1 else 1)) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }

    // Image source dialog
    if (showImageSourceDialog) {
        ImageSourceDialog(
            onGalleryClick = {
                galleryLauncher.launch("image/*")
            },
            onCameraClick = {
                val tempUri = createTempImageFile()
                cameraImageUri = tempUri
                cameraLauncher.launch(tempUri)
            },
            onDismiss = onDismissImageSource
        )
    }

    // Document options dialog
    showDocumentOptions?.let { document ->
        DocumentOptionsDialog(
            onViewClick = { onViewDocument(document) },
            onDeleteClick = { onDeleteDocument(document) },
            onDismiss = onDismissDocumentOptions
        )
    }
}

@Composable
private fun DocumentCard(
    document: Document,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(document.url)
                .crossfade(true)
                .build(),
            contentDescription = document.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Success indicator
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Uploaded",
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .size(20.dp)
        )
    }
}

@Composable
private fun UploadPlaceholder(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Upload",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "Upload",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun UploadingCard(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            strokeWidth = 3.dp
        )
    }
}