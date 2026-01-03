package com.anugraha.stays.data.repository

import android.util.Log
import com.anugraha.stays.data.remote.api.AnugrahaApi
import com.anugraha.stays.data.remote.dto.StatementRequest
import com.anugraha.stays.data.remote.dto.toDomain
import com.anugraha.stays.domain.model.Statement
import com.anugraha.stays.domain.repository.StatementRepository
import com.anugraha.stays.util.NetworkResult
import java.time.LocalDate
import javax.inject.Inject

class StatementRepositoryImpl @Inject constructor(
    private val api: AnugrahaApi
) : StatementRepository {

    override suspend fun generateStatement(
        startDate: LocalDate,
        endDate: LocalDate
    ): NetworkResult<Statement> {
        return try {
            Log.d("StatementRepo", "========== GENERATE STATEMENT ==========")
            Log.d("StatementRepo", "Start Date: $startDate")
            Log.d("StatementRepo", "End Date: $endDate")

            val request = StatementRequest(
                startDate = startDate.toString(),
                endDate = endDate.toString()
            )

            Log.d("StatementRepo", "Request: $request")

            val response = api.generateStatement(request)

            Log.d("StatementRepo", "Response code: ${response.code()}")
            Log.d("StatementRepo", "Response body: ${response.body()}")

            if (response.isSuccessful) {
                val statementResponse = response.body()

                if (statementResponse != null && statementResponse.success && statementResponse.data != null) {
                    val data = statementResponse.data

                    // Safely map reservations with null checks
// Fetch complete reservation details for each reservation
                    val reservations = data.reservations
                        ?.mapNotNull { incompleteDto ->
                            try {
                                if (incompleteDto.id == null) {
                                    Log.e("StatementRepo", "❌ Reservation ID is null")
                                    return@mapNotNull null
                                }

                                Log.d("StatementRepo", "========== Fetching Full Reservation ==========")
                                Log.d("StatementRepo", "Reservation ID: ${incompleteDto.id}")

                                // Fetch full reservation from /api/reservations/{id}
                                val fullResponse = api.getReservationById(incompleteDto.id)

                                if (fullResponse.isSuccessful) {
                                    val fullDto = fullResponse.body()

                                    if (fullDto != null) {
                                        Log.d("StatementRepo", "✅ Got full reservation data")
                                        Log.d("StatementRepo", "   Primary Guest: ${fullDto.primaryGuest?.fullName}")
                                        Log.d("StatementRepo", "   Guest: ${fullDto.guest?.fullName}")

                                        // Use the FULL DTO which has complete guest data
                                        val reservation = fullDto.toDomain()

                                        if (reservation != null) {
                                            Log.d("StatementRepo", "✅ Mapped to: ${reservation.primaryGuest.fullName}")
                                        }
                                        Log.d("StatementRepo", "===============================================")

                                        reservation
                                    } else {
                                        Log.e("StatementRepo", "❌ Response body is null")
                                        incompleteDto.toDomain()
                                    }
                                } else {
                                    Log.e("StatementRepo", "❌ Failed to fetch: ${fullResponse.code()}")
                                    incompleteDto.toDomain()
                                }
                            } catch (e: Exception) {
                                Log.e("StatementRepo", "❌ Exception fetching reservation: ${e.message}", e)
                                incompleteDto.toDomain()
                            }
                        }
                        ?.filterNotNull()
                        ?: emptyList()

                    val statement = Statement(
                        totalRevenue = data.totalAmount,
                        totalBookings = data.totalReservations,
                        reservations = reservations
                    )

                    Log.d("StatementRepo", "✅ Statement generated successfully")
                    Log.d("StatementRepo", "Total Bookings: ${statement.totalBookings}")
                    Log.d("StatementRepo", "Total Revenue: ${statement.totalRevenue}")
                    Log.d("StatementRepo", "Reservations count: ${reservations.size}")

                    NetworkResult.Success(statement)
                } else {
                    Log.e("StatementRepo", "Response body is null or unsuccessful")
                    Log.e("StatementRepo", "Success: ${statementResponse?.success}")
                    Log.e("StatementRepo", "Data: ${statementResponse?.data}")
                    NetworkResult.Error("No data received from server")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("StatementRepo", "❌ Failed: ${response.code()} - $errorBody")
                NetworkResult.Error("Failed to generate statement: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("StatementRepo", "❌ Exception", e)
            NetworkResult.Error(e.message ?: "An error occurred")
        }
    }
}