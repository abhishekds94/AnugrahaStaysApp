package com.anugraha.stays.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.anugraha.stays.data.local.database.AppDatabase
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "anugraha_stays_database"
        )
            .addMigrations(MIGRATION_1_2)  // ADD THIS
            .fallbackToDestructiveMigration()
            .build()
    }

    // ADD THIS MIGRATION
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Update all AIRBNB bookings to have proper display name
            database.execSQL(
                """
            UPDATE external_bookings 
            SET summary = 'Booking from Airbnb' 
            WHERE source = 'AIRBNB'
            """
            )

            // Update all BOOKING_COM bookings
            database.execSQL(
                """
            UPDATE external_bookings 
            SET summary = 'Booking from Booking.com' 
            WHERE source = 'BOOKING_COM'
            """
            )
        }
    }
}