package com.anugraha.stays.data.remote.api

import com.anugraha.stays.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface AnugrahaApi {

    // Check-ins
    @GET("today-checkins")
    suspend fun getTodayCheckIns(): Response<CheckInResponse>

    @GET("today-checkouts")
    suspend fun getTodayCheckOuts(): Response<CheckInResponse>

    // Reservations
    @GET("reservations")
    suspend fun getReservations(
        @Query("per_page") perPage: Int = 10,
        @Query("page") page: Int = 1,
        @Query("sort_by") sortBy: String? = null,
        @Query("sort_order") sortOrder: String? = null,
        @Query("status") status: String? = null,
        @Query("check_in_date") checkInDate: String? = null
    ): Response<List<ReservationDto>>

    @GET("reservations/{id}")
    suspend fun getReservationById(
        @Path("id") id: Int
    ): Response<ReservationDto>

    // Accept/Decline booking
    @POST("reservation/{id}/accept")
    suspend fun acceptReservation(
        @Path("id") id: Int
    ): Response<MessageResponse>

    @POST("reservation/{id}/decline")
    suspend fun declineReservation(
        @Path("id") id: Int
    ): Response<MessageResponse>

    // Create admin booking
    @POST("reservation/booking")
    suspend fun createAdminBooking(
        @Body request: CreateBookingRequest
    ): Response<CreateBookingResponse>

    // Availability calendar
    @GET("availability-calendar")
    suspend fun getAvailability(): Response<List<AvailabilityDto>>

    @POST("availability-calendar/update")
    suspend fun updateAvailability(
        @Body request: AvailabilityUpdateRequest
    ): Response<MessageResponse>

    // Generate statement
    @POST("statement/generate")
    suspend fun generateStatement(
        @Body request: StatementRequest
    ): Response<StatementResponse>
}