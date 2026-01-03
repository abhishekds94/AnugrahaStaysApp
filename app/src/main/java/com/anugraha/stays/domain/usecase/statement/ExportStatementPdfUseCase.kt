package com.anugraha.stays.domain.usecase.statement

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.core.content.FileProvider
import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.util.NetworkResult
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ExportStatementPdfUseCase @Inject constructor() {

    suspend operator fun invoke(
        context: Context,
        startDate: LocalDate,
        endDate: LocalDate,
        reservations: List<Reservation>,
        totalRevenue: Double,
        totalBookings: Int
    ): NetworkResult<String> {
        return try {
            if (reservations.isEmpty()) {
                return NetworkResult.Error("No reservations to export")
            }

            val pdfDocument = PdfDocument()

            // A4 size in landscape for better table fit
            val pageWidth = 842  // A4 landscape width
            val pageHeight = 595 // A4 landscape height
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint()
            var yPos = 40f
            val leftMargin = 30f
            val lineHeight = 18f

            // Title
            paint.textSize = 16f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Anugraha Stays - Reservation Statement", leftMargin, yPos, paint)
            yPos += lineHeight + 5

            // Date range and generation time
            paint.textSize = 10f
            paint.typeface = Typeface.DEFAULT
            val dateFormat = DateTimeFormatter.ofPattern("MMM dd, yyyy")
            val timeFormat = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a")
            canvas.drawText(
                "Date Range: ${startDate.format(dateFormat)} to ${endDate.format(dateFormat)}",
                leftMargin,
                yPos,
                paint
            )
            yPos += lineHeight
            canvas.drawText(
                "Generated on: ${LocalDate.now().atTime(java.time.LocalTime.now()).format(timeFormat)}",
                leftMargin,
                yPos,
                paint
            )
            yPos += lineHeight + 10

            // Draw line
            canvas.drawLine(leftMargin, yPos, pageWidth - 30f, yPos, paint)
            yPos += 15

            // Table headers
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            val colDate = leftMargin
            val colDay = colDate + 55
            val colBooking = colDay + 50
            val colGuest = colBooking + 75
            val colPhone = colGuest + 80
            val colPlatform = colPhone + 75
            val colPaid = colPlatform + 70
            val colGuests = colPaid + 70
            val colRoom = colGuests + 45
            val colPets = colRoom + 75
            val colCheckin = colPets + 35
            val colPayment = colCheckin + 55
            val colTransport = colPayment + 95

            canvas.drawText("Date", colDate, yPos, paint)
            canvas.drawText("Day", colDay, yPos, paint)
            canvas.drawText("Booking No", colBooking, yPos, paint)
            canvas.drawText("Guest Name", colGuest, yPos, paint)
            canvas.drawText("Phone", colPhone, yPos, paint)
            canvas.drawText("Platform", colPlatform, yPos, paint)
            canvas.drawText("Paid", colPaid, yPos, paint)
            canvas.drawText("Guests", colGuests, yPos, paint)
            canvas.drawText("Room", colRoom, yPos, paint)
            canvas.drawText("Pets", colPets, yPos, paint)
            canvas.drawText("Check-in", colCheckin, yPos, paint)
            canvas.drawText("Payment Ref", colPayment, yPos, paint)
            canvas.drawText("Transport", colTransport, yPos, paint)
            yPos += 5

            // Line separator
            canvas.drawLine(leftMargin, yPos, pageWidth - 30f, yPos, paint)
            yPos += 12

            // Table rows
            paint.textSize = 7f
            paint.typeface = Typeface.DEFAULT
            val dayFormatter = DateTimeFormatter.ofPattern("EEEE")
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

            // Generate all dates in range
            var currentDate = startDate
            while (!currentDate.isAfter(endDate)) {
                val bookingsForDate = reservations.filter { it.checkInDate == currentDate }

                if (yPos > pageHeight - 40) {
                    pdfDocument.finishPage(page)
                    val newPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdfDocument.pages.size + 1).create()
                    val newPage = pdfDocument.startPage(newPageInfo)
                    yPos = 40f
                }

                if (bookingsForDate.isEmpty()) {
                    // Show date with no bookings
                    canvas.drawText(currentDate.format(dateFormatter), colDate, yPos, paint)
                    canvas.drawText(currentDate.format(dayFormatter), colDay, yPos, paint)
                    canvas.drawText("BLOCKED", colBooking, yPos, paint)
                    canvas.drawText("-", colGuest, yPos, paint)
                    canvas.drawText("-", colPhone, yPos, paint)

                    // Determine platform for blocked dates (you can enhance this logic)
                    val platform = "Booking.com" // Or derive from availability data
                    canvas.drawText(platform, colPlatform, yPos, paint)
                    canvas.drawText(platform, colPaid, yPos, paint)
                    canvas.drawText("-", colGuests, yPos, paint)

                    // Room info for blocked dates
                    canvas.drawText("2 Non A/C Deluxe", colRoom, yPos, paint) // Or derive from data
                    canvas.drawText("-", colPets, yPos, paint)
                    canvas.drawText("-", colCheckin, yPos, paint)
                    canvas.drawText("-", colPayment, yPos, paint)
                    canvas.drawText("No", colTransport, yPos, paint)
                    yPos += lineHeight
                } else {
                    // Show booking details
                    bookingsForDate.forEach { reservation ->
                        canvas.drawText(currentDate.format(dateFormatter), colDate, yPos, paint)
                        canvas.drawText(currentDate.format(dayFormatter), colDay, yPos, paint)
                        canvas.drawText(reservation.reservationNumber.take(12), colBooking, yPos, paint)
                        canvas.drawText(reservation.primaryGuest.fullName.take(15), colGuest, yPos, paint)
                        canvas.drawText(reservation.primaryGuest.phone, colPhone, yPos, paint)
                        canvas.drawText(reservation.bookingSource.name.take(10), colPlatform, yPos, paint)
                        canvas.drawText("Rs. ${"%.2f".format(reservation.totalAmount)}", colPaid, yPos, paint)
                        canvas.drawText((reservation.adults + reservation.kids).toString(), colGuests, yPos, paint)
                        canvas.drawText(reservation.room?.title?.take(15) ?: "N/A", colRoom, yPos, paint)
                        canvas.drawText(if (reservation.hasPet) "Yes" else "No", colPets, yPos, paint)
                        canvas.drawText(reservation.estimatedCheckInTime?.toString() ?: "-", colCheckin, yPos, paint)
                        canvas.drawText(reservation.paymentReference?.take(18) ?: "-", colPayment, yPos, paint)
                        canvas.drawText(reservation.transportService ?: "No", colTransport, yPos, paint)
                        yPos += lineHeight
                    }
                }

                currentDate = currentDate.plusDays(1)
            }

            yPos += 10
            canvas.drawLine(leftMargin, yPos, pageWidth - 30f, yPos, paint)
            yPos += 15

            // Summary
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Total Reservations: $totalBookings", leftMargin, yPos, paint)
            yPos += lineHeight
            canvas.drawText("Total Amount: Rs. ${"%.2f".format(totalRevenue)}", leftMargin, yPos, paint)

            pdfDocument.finishPage(page)

            // Save PDF
            val fileName = "statement_${startDate}_${endDate}.pdf"
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)

            FileOutputStream(file).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()

            // Open PDF
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)

            NetworkResult.Success(file.absolutePath)
        } catch (e: Exception) {
            android.util.Log.e("ExportStatementPdf", "Error creating PDF", e)
            NetworkResult.Error(e.message ?: "Failed to export PDF")
        }
    }
}