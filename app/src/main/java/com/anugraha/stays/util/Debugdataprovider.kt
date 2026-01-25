package com.anugraha.stays.util

import com.anugraha.stays.domain.model.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * Provides dummy data for testing and debugging purposes.
 * This should only be used in development/debug builds.
 */
object DebugDataProvider {

    /**
     * Generate dummy check-ins for today
     */
    fun getTodayCheckIns(): List<CheckIn> = listOf(
        CheckIn(
            reservation = createDummyReservation(
                id = 101,
                guestName = "Rajesh Kumar",
                phone = "9876543210",
                checkIn = LocalDate.now(),
                checkOut = LocalDate.now().plusDays(3),
                amount = 6750.0,
                source = BookingSource.DIRECT
            ),
            checkInTime = LocalTime.of(14, 30)
        ),
        CheckIn(
            reservation = createDummyReservation(
                id = 102,
                guestName = "Priya Sharma",
                phone = "9988776655",
                checkIn = LocalDate.now(),
                checkOut = LocalDate.now().plusDays(2),
                amount = 5500.0,
                source = BookingSource.WEBSITE
            ),
            checkInTime = LocalTime.of(15, 0)
        ),
        CheckIn(
            reservation = createDummyReservation(
                id = 103,
                guestName = "Amit Patel",
                phone = "9123456789",
                checkIn = LocalDate.now(),
                checkOut = LocalDate.now().plusDays(5),
                amount = 13750.0,
                source = BookingSource.AIRBNB
            ),
            checkInTime = LocalTime.of(16, 15)
        )
    )

    /**
     * Generate dummy check-outs for today
     */
    fun getTodayCheckOuts(): List<CheckOut> = listOf(
        CheckOut(
            reservation = createDummyReservation(
                id = 104,
                guestName = "Purushotham B C",
                phone = "9980421928",
                checkIn = LocalDate.now().minusDays(1),
                checkOut = LocalDate.now(),
                amount = 2750.0,
                source = BookingSource.DIRECT
            ),
            checkOutTime = LocalTime.of(11, 0)
        ),
        CheckOut(
            reservation = createDummyReservation(
                id = 105,
                guestName = "Sneha Reddy",
                phone = "9900112233",
                checkIn = LocalDate.now().minusDays(3),
                checkOut = LocalDate.now(),
                amount = 9750.0,
                source = BookingSource.BOOKING_COM
            ),
            checkOutTime = LocalTime.of(10, 30)
        )
    )

    /**
     * Generate dummy upcoming check-ins for this week
     */
    fun getUpcomingCheckIns(): List<WeekBooking> = listOf(
        WeekBooking(
            reservation = createDummyReservation(
                id = 106,
                guestName = "Vikram Singh",
                phone = "9876501234",
                checkIn = LocalDate.now().plusDays(2),
                checkOut = LocalDate.now().plusDays(5),
                amount = 9750.0,
                source = BookingSource.DIRECT
            ),
            dayOfWeek = LocalDate.now().plusDays(2).dayOfWeek,
            date = LocalDate.now().plusDays(2)
        ),
        WeekBooking(
            reservation = createDummyReservation(
                id = 107,
                guestName = "Lakshmi Iyer",
                phone = "9988001122",
                checkIn = LocalDate.now().plusDays(3),
                checkOut = LocalDate.now().plusDays(6),
                amount = 10250.0,
                source = BookingSource.WEBSITE
            ),
            dayOfWeek = LocalDate.now().plusDays(3).dayOfWeek,
            date = LocalDate.now().plusDays(3)
        ),
        WeekBooking(
            reservation = createDummyReservation(
                id = 108,
                guestName = "Mohammed Ali",
                phone = "9123009988",
                checkIn = LocalDate.now().plusDays(4),
                checkOut = LocalDate.now().plusDays(7),
                amount = 8250.0,
                source = BookingSource.AIRBNB
            ),
            dayOfWeek = LocalDate.now().plusDays(4).dayOfWeek,
            date = LocalDate.now().plusDays(4)
        )
    )

    /**
     * Generate dummy pending reservations
     */
    fun getPendingReservations(): List<Reservation> = listOf(
        createDummyReservation(
            id = 201,
            guestName = "Anjali Mehta",
            phone = "9876509876",
            checkIn = LocalDate.now().plusDays(5),
            checkOut = LocalDate.now().plusDays(8),
            amount = 9750.0,
            source = BookingSource.WEBSITE,
            status = ReservationStatus.PENDING
        ),
        createDummyReservation(
            id = 202,
            guestName = "Karthik Rao",
            phone = "9900998877",
            checkIn = LocalDate.now().plusDays(6),
            checkOut = LocalDate.now().plusDays(9),
            amount = 8250.0,
            source = BookingSource.DIRECT,
            status = ReservationStatus.PENDING
        )
    )

    /**
     * Generate dummy reservations list
     */
    fun getAllReservations(): List<Reservation> = listOf(
        // Confirmed bookings
        createDummyReservation(
            id = 301,
            guestName = "Arjun Desai",
            phone = "9876543211",
            checkIn = LocalDate.now().plusDays(1),
            checkOut = LocalDate.now().plusDays(4),
            amount = 9750.0,
            source = BookingSource.DIRECT,
            status = ReservationStatus.APPROVED
        ),
        createDummyReservation(
            id = 302,
            guestName = "Divya Nair",
            phone = "9988776656",
            checkIn = LocalDate.now().plusDays(3),
            checkOut = LocalDate.now().plusDays(6),
            amount = 10250.0,
            source = BookingSource.WEBSITE,
            status = ReservationStatus.APPROVED
        ),
        createDummyReservation(
            id = 303,
            guestName = "Suresh Babu",
            phone = "9123456788",
            checkIn = LocalDate.now().plusDays(7),
            checkOut = LocalDate.now().plusDays(10),
            amount = 8250.0,
            source = BookingSource.BOOKING_COM,
            status = ReservationStatus.COMPLETED
        ),
        // Pending bookings
        *getPendingReservations().toTypedArray()
    )

    /**
     * Helper to create a dummy reservation
     */
    private fun createDummyReservation(
        id: Int,
        guestName: String,
        phone: String,
        checkIn: LocalDate,
        checkOut: LocalDate,
        amount: Double,
        source: BookingSource,
        status: ReservationStatus = ReservationStatus.APPROVED
    ): Reservation {
        val names = guestName.split(" ")
        return Reservation(
            id = id,
            reservationNumber = "BK${id.toString().padStart(6, '0')}",
            primaryGuest = Guest(
                fullName = names.firstOrNull() ?: "Guest",
                email = "${guestName.lowercase().replace(" ", ".")}@example.com",
                phone = phone
            ),
            checkInDate = checkIn,
            checkOutDate = checkOut,
            adults = (2..5).random(),
            kids = (0..2).random(),
            hasPet = (0..10).random() > 7, // 30% chance of pet
            status = status,
            bookingSource = source,
            totalAmount = amount,
            room = Room(
                id = (1..3).random(),
                title = listOf("Deluxe Room", "Premium Suite", "Garden View Room").random(),
                description = "Comfortable accommodation with modern amenities",
                data = RoomData(airConditioned = (0..1).random() == 1)
            ),
            estimatedCheckInTime = if (status == ReservationStatus.APPROVED) {
                LocalTime.of((12..18).random(), (0..3).random() * 15)
            } else null,
            transactionId = if (status == ReservationStatus.APPROVED) "TXN${(100000..999999).random()}" else null,
            paymentStatus = if (status == ReservationStatus.APPROVED) "Paid" else "Pending",
            transportService = if ((0..10).random() > 7) "Yes" else "No",
            paymentReference = if (status == ReservationStatus.APPROVED) "REF${(1000..9999).random()}" else null
        )
    }
}