package com.anugraha.stays.domain.repository

import com.anugraha.stays.domain.model.CheckIn
import com.anugraha.stays.domain.model.CheckOut
import com.anugraha.stays.domain.model.Reservation
import com.anugraha.stays.domain.model.WeekBooking
import com.anugraha.stays.util.NetworkResult

interface DashboardRepository {
    suspend fun getTodayCheckIns(): NetworkResult<List<CheckIn>>
    suspend fun getTodayCheckOuts(): NetworkResult<List<CheckOut>>
    suspend fun getThisWeekBookings(): NetworkResult<List<WeekBooking>>
    suspend fun getPendingReservations(): NetworkResult<List<Reservation>>
}