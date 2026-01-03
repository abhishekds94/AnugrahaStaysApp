package com.anugraha.stays.di

import com.anugraha.stays.data.local.preferences.UserPreferences
import com.anugraha.stays.data.remote.api.AnugrahaApi
import com.anugraha.stays.data.remote.firebase.FirebaseAuthDataSource
import com.anugraha.stays.data.repository.*
import com.anugraha.stays.domain.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuthDataSource: FirebaseAuthDataSource,
        userPreferences: UserPreferences
    ): AuthRepository {
        return AuthRepositoryImpl(firebaseAuthDataSource, userPreferences)
    }

    @Provides
    @Singleton
    fun provideDashboardRepository(api: AnugrahaApi): DashboardRepository {
        return DashboardRepositoryImpl(api)
    }

    @Provides
    @Singleton
    fun provideReservationRepository(api: AnugrahaApi): ReservationRepository {
        return ReservationRepositoryImpl(api)
    }

    @Provides
    @Singleton
    fun provideAvailabilityRepository(api: AnugrahaApi): AvailabilityRepository {
        return AvailabilityRepositoryImpl(api)
    }

    @Provides
    @Singleton
    fun provideBookingRepository(api: AnugrahaApi): BookingRepository {
        return BookingRepositoryImpl(api)
    }

    @Provides
    @Singleton
    fun provideStatementRepository(api: AnugrahaApi): StatementRepository {
        return StatementRepositoryImpl(api)
    }
}