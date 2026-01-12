package com.anugraha.stays.di

import com.anugraha.stays.data.local.database.AppDatabase
import com.anugraha.stays.data.local.database.dao.ExternalBookingDao
import com.anugraha.stays.data.local.preferences.UserPreferences
import com.anugraha.stays.data.remote.api.AnugrahaApi
import com.anugraha.stays.data.remote.firebase.FirebaseAuthDataSource
import com.anugraha.stays.data.remote.ical.ICalParser
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

    // AUTH REPOSITORY - Uses Firebase (NO CHANGES NEEDED)
    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuthDataSource: FirebaseAuthDataSource,
        userPreferences: UserPreferences
    ): AuthRepository {
        return AuthRepositoryImpl(firebaseAuthDataSource, userPreferences)
    }

    // DASHBOARD REPOSITORY - ADD iCalSyncRepository
    @Provides
    @Singleton
    fun provideDashboardRepository(
        api: AnugrahaApi,
        iCalSyncRepository: ICalSyncRepository
    ): DashboardRepository {
        return DashboardRepositoryImpl(api, iCalSyncRepository)
    }

    // RESERVATION REPOSITORY - ADD iCalSyncRepository
    @Provides
    @Singleton
    fun provideReservationRepository(
        api: AnugrahaApi,
        iCalSyncRepository: ICalSyncRepository
    ): ReservationRepository {
        return ReservationRepositoryImpl(api, iCalSyncRepository)
    }

    // AVAILABILITY REPOSITORY - NO CHANGES
    @Provides
    @Singleton
    fun provideAvailabilityRepository(
        api: AnugrahaApi
    ): AvailabilityRepository {
        return AvailabilityRepositoryImpl(api)
    }

    // BOOKING REPOSITORY - NO CHANGES
    @Provides
    @Singleton
    fun provideBookingRepository(
        api: AnugrahaApi
    ): BookingRepository {
        return BookingRepositoryImpl(api)
    }

    // STATEMENT REPOSITORY - ADD iCalSyncRepository
    @Provides
    @Singleton
    fun provideStatementRepository(
        api: AnugrahaApi,
        iCalSyncRepository: ICalSyncRepository
    ): StatementRepository {
        return StatementRepositoryImpl(api, iCalSyncRepository)
    }

    // ==================== ICAL SYNC PROVIDERS ====================

    @Provides
    @Singleton
    fun provideICalParser(): ICalParser {
        return ICalParser()
    }

    @Provides
    @Singleton
    fun provideExternalBookingDao(database: AppDatabase): ExternalBookingDao {
        return database.externalBookingDao()
    }

    @Provides
    @Singleton
    fun provideICalSyncRepository(
        parser: ICalParser,
        dao: ExternalBookingDao
    ): ICalSyncRepository {
        return ICalSyncRepositoryImpl(parser, dao)
    }
}