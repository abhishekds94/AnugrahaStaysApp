package com.anugraha.stays.util

import com.anugraha.stays.domain.model.*
import java.time.LocalDate

/**
 * Debug data generator for testing balance tracking features
 * Creates test bookings in March 2027 with various scenarios
 */
object BalanceTestDataGenerator {

    /**
     * Generate test reservations for balance tracking features
     * All bookings are in March 2027 to avoid interfering with production data
     */
    fun generateTestReservations(): List<Reservation> {
        val testReservations = mutableListOf<Reservation>()

        // Test Scenario 1: Underpaid booking - Check-in TODAY (March 1)
        testReservations.add(
            createTestReservation(
                id = 9001,
                reservationNumber = "TEST-UNDERPAID-CHECKIN",
                primaryGuest = Guest(
                    fullName = "John Underpaid",
                    phone = "9999000001",
                    email = "john.test@example.com"
                ),
                checkInDate = LocalDate.of(2027, 3, 1),
                checkOutDate = LocalDate.of(2027, 3, 3), // 2 nights
                adults = 4,
                kids = 0,
                hasPet = false,
                room = createAcRoom(),
                totalAmount = 3000.0, // Underpaid (should be 5,500)
                status = ReservationStatus.APPROVED,
                bookingSource = BookingSource.DIRECT
            )
        )

        // Test Scenario 2: Underpaid booking - Check-out TODAY (March 1)
        testReservations.add(
            createTestReservation(
                id = 9002,
                reservationNumber = "TEST-UNDERPAID-CHECKOUT",
                primaryGuest = Guest(
                    fullName = "Jane Checkout",
                    phone = "9999000002",
                    email = "jane.test@example.com"
                ),
                checkInDate = LocalDate.of(2027, 2, 27),
                checkOutDate = LocalDate.of(2027, 3, 1), // Checking out today (2 nights)
                adults = 6, // 2 extra guests
                kids = 0,
                hasPet = true, // Has pet
                room = createNonAcRoom(),
                totalAmount = 4000.0, // Underpaid (should be 6,700)
                status = ReservationStatus.APPROVED,
                bookingSource = BookingSource.DIRECT
            )
        )

        // Test Scenario 3: Perfectly settled booking - Check-in today
        testReservations.add(
            createTestReservation(
                id = 9003,
                reservationNumber = "TEST-SETTLED",
                primaryGuest = Guest(
                    fullName = "Mike Settled",
                    phone = "9999000003",
                    email = "mike.test@example.com"
                ),
                checkInDate = LocalDate.of(2027, 3, 1),
                checkOutDate = LocalDate.of(2027, 3, 3), // 2 nights
                adults = 4,
                kids = 0,
                hasPet = false,
                room = createNonAcRoom(),
                totalAmount = 4500.0, // Exactly correct (2 × 2,250)
                status = ReservationStatus.APPROVED,
                bookingSource = BookingSource.DIRECT
            )
        )

        // Test Scenario 4: Overpaid booking - Check-in today
        testReservations.add(
            createTestReservation(
                id = 9004,
                reservationNumber = "TEST-OVERPAID",
                primaryGuest = Guest(
                    fullName = "Sarah Overpaid",
                    phone = "9999000004",
                    email = "sarah.test@example.com"
                ),
                checkInDate = LocalDate.of(2027, 3, 1),
                checkOutDate = LocalDate.of(2027, 3, 2), // 1 night
                adults = 3,
                kids = 0,
                hasPet = false,
                room = createNonAcRoom(),
                totalAmount = 5000.0, // Overpaid (should be 2,250)
                status = ReservationStatus.APPROVED,
                bookingSource = BookingSource.DIRECT
            )
        )

        // Test Scenario 5: Upcoming booking with pending balance (March 5)
        testReservations.add(
            createTestReservation(
                id = 9005,
                reservationNumber = "TEST-UPCOMING-UNDERPAID",
                primaryGuest = Guest(
                    fullName = "Tom Upcoming",
                    phone = "9999000005",
                    email = "tom.test@example.com"
                ),
                checkInDate = LocalDate.of(2027, 3, 5),
                checkOutDate = LocalDate.of(2027, 3, 8), // 3 nights
                adults = 5, // 1 extra guest
                kids = 0,
                hasPet = true,
                room = createAcRoom(),
                totalAmount = 7000.0, // Underpaid (should be 10,650)
                status = ReservationStatus.APPROVED,
                bookingSource = BookingSource.DIRECT
            )
        )

        // Test Scenario 6: Multiple extras - AC + extra guests + pet
        testReservations.add(
            createTestReservation(
                id = 9006,
                reservationNumber = "TEST-ALL-EXTRAS",
                primaryGuest = Guest(
                    fullName = "Emma AllExtras",
                    phone = "9999000006",
                    email = "emma.test@example.com"
                ),
                checkInDate = LocalDate.of(2027, 3, 10),
                checkOutDate = LocalDate.of(2027, 3, 13), // 3 nights
                adults = 7, // 3 extra guests
                kids = 0,
                hasPet = true,
                room = createAcRoom(),
                totalAmount = 8000.0, // Underpaid (should be 12,450)
                status = ReservationStatus.APPROVED,
                bookingSource = BookingSource.DIRECT
            )
        )

        // Test Scenario 7: External booking (should NOT show balance warnings)
        testReservations.add(
            createTestReservation(
                id = 9007,
                reservationNumber = "TEST-EXTERNAL",
                primaryGuest = Guest(
                    fullName = "Airbnb Guest",
                    phone = "",
                    email = null
                ),
                checkInDate = LocalDate.of(2027, 3, 1),
                checkOutDate = LocalDate.of(2027, 3, 4),
                adults = 4,
                kids = 0,
                hasPet = false,
                room = createNonAcRoom(),
                totalAmount = 3000.0,
                status = ReservationStatus.APPROVED,
                bookingSource = BookingSource.AIRBNB
            )
        )

        // Test Scenario 8: Pending status (should NOT show in dashboard)
        testReservations.add(
            createTestReservation(
                id = 9008,
                reservationNumber = "TEST-PENDING",
                primaryGuest = Guest(
                    fullName = "Pending Guest",
                    phone = "9999000008",
                    email = "pending.test@example.com"
                ),
                checkInDate = LocalDate.of(2027, 3, 1),
                checkOutDate = LocalDate.of(2027, 3, 3),
                adults = 4,
                kids = 0,
                hasPet = false,
                room = createNonAcRoom(),
                totalAmount = 2000.0,
                status = ReservationStatus.PENDING, // Should be filtered out
                bookingSource = BookingSource.DIRECT
            )
        )

        // Test Scenario 9: Completed checkout with pending balance
        testReservations.add(
            createTestReservation(
                id = 9009,
                reservationNumber = "TEST-COMPLETED-BALANCE",
                primaryGuest = Guest(
                    fullName = "Lisa Completed",
                    phone = "9999000009",
                    email = "lisa.test@example.com"
                ),
                checkInDate = LocalDate.of(2027, 2, 26),
                checkOutDate = LocalDate.of(2027, 3, 1), // Checking out today
                adults = 5,
                kids = 2, // Kids included
                hasPet = false,
                room = createAcRoom(),
                totalAmount = 6000.0, // Underpaid (should be 8,250)
                status = ReservationStatus.COMPLETED,
                bookingSource = BookingSource.DIRECT
            )
        )

        // Test Scenario 10: Long stay with max extras
        testReservations.add(
            createTestReservation(
                id = 9010,
                reservationNumber = "TEST-LONG-STAY",
                primaryGuest = Guest(
                    fullName = "David LongStay",
                    phone = "9999000010",
                    email = "david.test@example.com"
                ),
                checkInDate = LocalDate.of(2027, 3, 15),
                checkOutDate = LocalDate.of(2027, 3, 22), // 7 nights
                adults = 8, // 4 extra guests
                kids = 0,
                hasPet = true,
                room = createAcRoom(),
                totalAmount = 20000.0, // Underpaid (should be 31,150)
                status = ReservationStatus.APPROVED,
                bookingSource = BookingSource.DIRECT
            )
        )

        return testReservations
    }

    /**
     * Helper function to create test reservation
     */
    private fun createTestReservation(
        id: Int,
        reservationNumber: String,
        primaryGuest: Guest,
        checkInDate: LocalDate,
        checkOutDate: LocalDate,
        adults: Int,
        kids: Int,
        hasPet: Boolean,
        room: Room?,
        totalAmount: Double,
        status: ReservationStatus,
        bookingSource: BookingSource
    ): Reservation {
        return Reservation(
            id = id,
            reservationNumber = reservationNumber,
            primaryGuest = primaryGuest,
            checkInDate = checkInDate,
            checkOutDate = checkOutDate,
            adults = adults,
            kids = kids,
            hasPet = hasPet,
            room = room,
            totalAmount = totalAmount,
            status = status,
            bookingSource = bookingSource
        )
    }

    /**
     * Create AC room for testing
     */
    private fun createAcRoom(): Room {
        return Room(
            id = 9901,
            title = "Test AC Room",
            description = "TODO()",
            data = RoomData(
                airConditioned = true
            ),
        )
    }

    /**
     * Create Non-AC room for testing
     */
    private fun createNonAcRoom(): Room {
        return Room(
            id = 9902,
            title = "Test AC Room",
            description = "TODO()",
            data = RoomData(
                airConditioned = false
            ),
        )
    }
}

/**
 * Expected calculations for test scenarios (for verification)
 */
object BalanceTestExpectedValues {

    data class ExpectedBalance(
        val scenarioName: String,
        val calculatedAmount: Double,
        val totalAmount: Double,
        val pendingBalance: Double,
        val description: String
    )

    val expectedBalances = listOf(
        ExpectedBalance(
            scenarioName = "Underpaid Check-in (John)",
            calculatedAmount = 5500.0, // 2 nights × (2,250 + 500 AC)
            totalAmount = 3000.0,
            pendingBalance = 2500.0,
            description = "2 nights, 4 guests, AC room"
        ),
        ExpectedBalance(
            scenarioName = "Underpaid Check-out (Jane)",
            calculatedAmount = 8700.0, // 2 nights × (2,250 + 600 extra guests + 500 pet)
            totalAmount = 5000.0,
            pendingBalance = 3700.0,
            description = "2 nights, 6 guests (2 extra), pet"
        ),
        ExpectedBalance(
            scenarioName = "Settled (Mike)",
            calculatedAmount = 4500.0, // 2 nights × 2,250
            totalAmount = 4500.0,
            pendingBalance = 0.0,
            description = "2 nights, 4 guests, no extras - PERFECTLY SETTLED"
        ),
        ExpectedBalance(
            scenarioName = "Overpaid (Sarah)",
            calculatedAmount = 2250.0, // 1 night × 2,250
            totalAmount = 5000.0,
            pendingBalance = -2750.0,
            description = "1 night, 3 guests - OVERPAID"
        ),
        ExpectedBalance(
            scenarioName = "Upcoming Underpaid (Tom)",
            calculatedAmount = 11550.0, // 3 nights × (2,250 + 500 AC + 300 extra + 500 pet)
            totalAmount = 8000.0,
            pendingBalance = 3550.0,
            description = "3 nights, 5 guests (1 extra), AC, pet"
        ),
        ExpectedBalance(
            scenarioName = "All Extras (Emma)",
            calculatedAmount = 13950.0, // 3 nights × (2,250 + 500 AC + 900 extra + 500 pet)
            totalAmount = 10000.0,
            pendingBalance = 3950.0,
            description = "3 nights, 7 guests (3 extra), AC, pet"
        ),
        ExpectedBalance(
            scenarioName = "Completed Checkout (Lisa)",
            calculatedAmount = 8250.0, // 3 nights × (2,250 + 500 AC)
            totalAmount = 6000.0,
            pendingBalance = 2250.0,
            description = "3 nights, 5 adults + 2 kids, AC"
        ),
        ExpectedBalance(
            scenarioName = "Long Stay (David)",
            calculatedAmount = 32550.0, // 7 nights × (2,250 + 500 AC + 1,200 extra + 500 pet)
            totalAmount = 20000.0,
            pendingBalance = 12550.0,
            description = "7 nights, 8 guests (4 extra), AC, pet - HUGE UNDERPAYMENT"
        )
    )
}