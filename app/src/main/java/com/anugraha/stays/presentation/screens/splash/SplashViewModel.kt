package com.anugraha.stays.presentation.screens.splash

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anugraha.stays.domain.repository.AuthRepository
import com.anugraha.stays.domain.usecase.ical.SyncICalFeedsUseCase  // ADD THIS
import com.anugraha.stays.util.NetworkResult  // ADD THIS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncICalFeedsUseCase: SyncICalFeedsUseCase  // ADD THIS
) : ViewModel() {

    suspend fun isUserLoggedIn(): Boolean {
        return authRepository.isUserLoggedIn()
    }

    // ADD THIS FUNCTION - Call this from SplashScreen after checking auth
    fun syncICalFeedsInBackground() {
        viewModelScope.launch {
            Log.d("SplashVM", "")
            Log.d("SplashVM", "╔═══════════════════════════════════════════╗")
            Log.d("SplashVM", "║     APP START - iCAL SYNC INITIATED       ║")
            Log.d("SplashVM", "╚═══════════════════════════════════════════╝")
            Log.d("SplashVM", "")

            when (val result = syncICalFeedsUseCase()) {
                is NetworkResult.Success -> {
                    Log.d("SplashVM", "")
                    Log.d("SplashVM", "╔═══════════════════════════════════════════╗")
                    Log.d("SplashVM", "║         ✅ iCAL SYNC SUCCESS              ║")
                    Log.d("SplashVM", "╚═══════════════════════════════════════════╝")
                    Log.d("SplashVM", "")
                    Log.d("SplashVM", "📊 SYNCED BOOKINGS: ${result.data.size}")
                    Log.d("SplashVM", "")

                    result.data.forEachIndexed { index, reservation ->
                        Log.d("SplashVM", "🏨 Booking #${index + 1}:")
                        Log.d("SplashVM", "   Source: ${reservation.bookingSource.displayName()}")
                        Log.d("SplashVM", "   Guest: ${reservation.primaryGuest.fullName}")
                        Log.d("SplashVM", "   Reservation #: ${reservation.reservationNumber}")
                        Log.d("SplashVM", "   Check-in: ${reservation.checkInDate}")
                        Log.d("SplashVM", "   Check-out: ${reservation.checkOutDate}")
                        Log.d("SplashVM", "   Status: ${reservation.status}")
                        Log.d("SplashVM", "   Room: ${reservation.room?.title ?: "N/A"}")
                        Log.d("SplashVM", "")
                    }

                    // Group by month
                    val byMonth = result.data.groupBy {
                        "${it.checkInDate.month} ${it.checkInDate.year}"
                    }

                    Log.d("SplashVM", "📅 BOOKINGS BY MONTH:")
                    byMonth.forEach { (month, bookings) ->
                        Log.d("SplashVM", "   $month: ${bookings.size} booking(s)")
                    }

                    Log.d("SplashVM", "")
                    Log.d("SplashVM", "═══════════════════════════════════════════")
                }
                is NetworkResult.Error -> {
                    Log.e("SplashVM", "")
                    Log.e("SplashVM", "╔═══════════════════════════════════════════╗")
                    Log.e("SplashVM", "║         ❌ iCAL SYNC FAILED               ║")
                    Log.e("SplashVM", "╚═══════════════════════════════════════════╝")
                    Log.e("SplashVM", "")
                    Log.e("SplashVM", "Error: ${result.message}")
                    Log.e("SplashVM", "")
                    Log.e("SplashVM", "═══════════════════════════════════════════")
                }
                NetworkResult.Loading -> {}
            }
        }
    }
}