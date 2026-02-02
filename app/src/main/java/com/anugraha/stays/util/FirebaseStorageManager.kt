package com.anugraha.stays.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.anugraha.stays.domain.model.CheckIn
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages document uploads to Firebase Storage with compression
 */
@Singleton
class FirebaseStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    private val storageRef: StorageReference = storage.reference

    /**
     * Upload image to Firebase Storage with compression
     * @param uri Image URI from gallery or camera
     * @param reservationId Reservation ID to organize uploads
     * @return Download URL of uploaded image
     */
    suspend fun uploadDocument(
        uri: Uri,
        checkInDate: LocalDate,
        guestName: String
    ): Result<String> {
        return try {
            // Read and compress image
            val compressedBytes = compressImage(uri)

            // Generate unique filename
            val filename = "${UUID.randomUUID()}.jpg"
            val path = "reservations/$checkInDate/$guestName/documents/$filename"

            // Upload to Firebase Storage
            val fileRef = storageRef.child(path)
            fileRef.putBytes(compressedBytes).await()

            // Get download URL
            val downloadUrl = fileRef.downloadUrl.await().toString()

            Result.success(downloadUrl)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Delete document from Firebase Storage
     * @param downloadUrl Download URL of the document
     */
    suspend fun deleteDocument(downloadUrl: String): Result<Unit> {
        return try {
            val fileRef = storage.getReferenceFromUrl(downloadUrl)
            fileRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Compress image without losing too much quality
     * Uses JPEG compression with quality 85
     */
    private fun compressImage(uri: Uri): ByteArray {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        // Calculate scaling if image is too large
        val maxDimension = 1920 // Max width or height
        val scaledBitmap = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
            val scale = maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
            val newWidth = (bitmap.width * scale).toInt()
            val newHeight = (bitmap.height * scale).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }

        // Compress to JPEG with quality 85 (good balance between quality and size)
        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val compressedBytes = outputStream.toByteArray()

        // Clean up
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }
        bitmap.recycle()
        outputStream.close()

        return compressedBytes
    }

    /**
     * Save URI to temporary file (for camera captures)
     */
    fun createTempImageFile(): File {
        val filename = "temp_${System.currentTimeMillis()}.jpg"
        return File(context.cacheDir, filename)
    }
}