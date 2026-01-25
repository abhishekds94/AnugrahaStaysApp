package com.anugraha.stays.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.debugDataStore: DataStore<Preferences> by preferencesDataStore(name = "debug_preferences")

/**
 * Manages debug mode preferences using DataStore.
 * This is used to toggle between real production data and dummy debug data.
 */
@Singleton
class DebugPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.debugDataStore

    companion object {
        private val USE_DEBUG_DATA = booleanPreferencesKey("use_debug_data")
    }

    /**
     * Flow that emits the current debug mode state
     */
    val useDebugData: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[USE_DEBUG_DATA] ?: false  // Default to production data
    }

    /**
     * Enable or disable debug mode
     */
    suspend fun setDebugMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[USE_DEBUG_DATA] = enabled
        }
    }

    /**
     * Toggle debug mode on/off
     */
    suspend fun toggleDebugMode() {
        dataStore.edit { preferences ->
            val current = preferences[USE_DEBUG_DATA] ?: false
            preferences[USE_DEBUG_DATA] = !current
        }
    }
}