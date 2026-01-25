package com.anugraha.stays.data.repository

import android.util.Log
import com.anugraha.stays.data.remote.api.AnugrahaApi
import com.anugraha.stays.data.remote.dto.AvailabilityUpdateRequest
import com.anugraha.stays.data.remote.dto.toDomain
import com.anugraha.stays.domain.model.Availability
import com.anugraha.stays.domain.model.AvailabilityStatus
import com.anugraha.stays.domain.model.availabilityStatusToApiString  // Import top-level function
import com.anugraha.stays.domain.repository.AvailabilityRepository
import com.anugraha.stays.util.NetworkResult
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

class AvailabilityRepositoryImpl @Inject constructor(
    private val api: AnugrahaApi
) : AvailabilityRepository {

    override suspend fun getAvailabilityForMonth(yearMonth: YearMonth): NetworkResult<List<Availability>> {
        return try {
            val response = api.getAvailability()

            if (response.isSuccessful) {
                val allAvailability = response.body()
                    ?.mapNotNull { it.toDomain() }
                    ?: emptyList()

                val filtered = allAvailability.filter { availability ->
                    availability.date.year == yearMonth.year &&
                            availability.date.monthValue == yearMonth.monthValue
                }
                NetworkResult.Success(filtered)
            } else {
                Log.e("AvailabilityRepo", "Error: ${response.code()}")
                NetworkResult.Error("Failed to fetch availability: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("AvailabilityRepo", "Exception: ${e.message}", e)
            NetworkResult.Error(e.message ?: "An error occurred")
        }
    }

    override suspend fun getAvailabilityForDate(date: LocalDate): NetworkResult<Availability?> {
        return try {
            val response = api.getAvailability()
            if (response.isSuccessful) {
                val availability = response.body()
                    ?.mapNotNull { it.toDomain() }
                    ?.find { it.date == date }

                NetworkResult.Success(availability)
            } else {
                NetworkResult.Error("Failed to fetch availability: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "An error occurred")
        }
    }

    override suspend fun updateAvailability(
        date: LocalDate,
        status: AvailabilityStatus,
        roomId: Int?
    ): NetworkResult<Unit> {
        return try {

            // Use the top-level function to convert status
            val apiStatus = availabilityStatusToApiString(status)

            val dateRange = "$date - $date"

            val request = AvailabilityUpdateRequest(
                dateRange = dateRange,
                status = apiStatus,
                roomId = roomId
            )

            val response = api.updateAvailability(request)

            val responseBody = response.body()

            val errorBody = response.errorBody()?.string()
            if (errorBody != null && errorBody.isNotEmpty()) {
                android.util.Log.e("AvailabilityRepo", "Error body: $errorBody")
            }

            android.util.Log.d("AvailabilityRepo", "Response headers: ${response.headers()}")

            if (response.isSuccessful) {
                android.util.Log.d("AvailabilityRepo", "✅ Update availability SUCCESS")
                NetworkResult.Success(Unit)
            } else {
                val errorMessage = "Failed to update availability: ${response.code()} ${response.message()} - $errorBody"
                android.util.Log.e("AvailabilityRepo", "❌ $errorMessage")
                NetworkResult.Error(errorMessage)
            }
        } catch (e: Exception) {
            android.util.Log.e("AvailabilityRepo", "Exception type: ${e.javaClass.simpleName}")
            android.util.Log.e("AvailabilityRepo", "Exception message: ${e.message}")
            NetworkResult.Error(e.message ?: "An error occurred")
        }
    }
}