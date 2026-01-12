package com.anugraha.stays.data.repository

import android.util.Log
import com.anugraha.stays.data.remote.api.AnugrahaApi
import com.anugraha.stays.data.remote.dto.StatementRequest
import com.anugraha.stays.data.remote.dto.toDomain
import com.anugraha.stays.domain.model.Statement
import com.anugraha.stays.domain.repository.ICalSyncRepository  // ADD THIS
import com.anugraha.stays.domain.repository.StatementRepository
import com.anugraha.stays.util.NetworkResult
import java.time.LocalDate
import javax.inject.Inject

class StatementRepositoryImpl @Inject constructor(
    private val api: AnugrahaApi,
    private val iCalSyncRepository: ICalSyncRepository  // ADD THIS
) : StatementRepository {

    override suspend fun generateStatement(
        startDate: LocalDate,
        endDate: LocalDate
    ): NetworkResult<Statement> {
        return try {
            Log.d("StatementRepo", "Generating statement from $startDate to $endDate")

            val request = StatementRequest(
                startDate = startDate.toString(),
                endDate = endDate.toString()
            )

            val response = api.generateStatement(request)

            if (response.isSuccessful) {
                val statementResponse = response.body()

                if (statementResponse != null && statementResponse.success && statementResponse.data != null) {
                    val data = statementResponse.data

                    // Fetch API reservations with full details
                    val apiReservations = data.reservations
                        ?.mapNotNull { incompleteDto ->
                            try {
                                if (incompleteDto.id == null) return@mapNotNull null

                                val fullResponse = api.getReservationById(incompleteDto.id)
                                if (fullResponse.isSuccessful) {
                                    fullResponse.body()?.toDomain()
                                } else {
                                    incompleteDto.toDomain()
                                }
                            } catch (e: Exception) {
                                incompleteDto.toDomain()
                            }
                        }
                        ?.filterNotNull()
                        ?: emptyList()

                    // GET EXTERNAL BOOKINGS in date range
                    val externalBookings = iCalSyncRepository.getExternalBookings()
                        .filter { reservation ->
                            reservation.checkInDate >= startDate &&
                                    reservation.checkInDate <= endDate
                        }

                    // MERGE
                    val allReservations = apiReservations + externalBookings

                    // RECALCULATE TOTALS
                    val totalRevenue = allReservations.sumOf { it.totalAmount }
                    val totalBookings = allReservations.size

                    val statement = Statement(
                        totalRevenue = totalRevenue,
                        totalBookings = totalBookings,
                        reservations = allReservations
                    )

                    Log.d("StatementRepo", "Statement: ${statement.totalBookings} bookings, ₹${statement.totalRevenue}")
                    NetworkResult.Success(statement)
                } else {
                    NetworkResult.Error("No data received from server")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("StatementRepo", "Failed: ${response.code()} - $errorBody")
                NetworkResult.Error("Failed to generate statement: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("StatementRepo", "Exception", e)
            NetworkResult.Error(e.message ?: "An error occurred")
        }
    }
}