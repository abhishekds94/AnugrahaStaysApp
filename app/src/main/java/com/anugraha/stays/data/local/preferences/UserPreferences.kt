package com.anugraha.stays.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_UID = stringPreferencesKey("user_uid")

        // Remember me credentials
        val REMEMBER_ME = booleanPreferencesKey("remember_me")
        val SAVED_USERNAME = stringPreferencesKey("saved_username")
        val SAVED_PASSWORD = stringPreferencesKey("saved_password")
    }

    // ========== Auth Token ==========

    suspend fun saveAuthToken(token: String) {
        dataStore.edit { prefs ->
            prefs[AUTH_TOKEN] = token
        }
    }

    fun getAuthToken(): Flow<String?> {
        return dataStore.data.map { prefs ->
            prefs[AUTH_TOKEN]
        }
    }

    // ========== User Email ==========

    suspend fun saveUserEmail(email: String) {
        dataStore.edit { prefs ->
            prefs[USER_EMAIL] = email
        }
    }

    fun getUserEmail(): Flow<String?> {
        return dataStore.data.map { prefs ->
            prefs[USER_EMAIL]
        }
    }

    // ========== User UID ==========

    suspend fun saveUserUid(uid: String) {
        dataStore.edit { prefs ->
            prefs[USER_UID] = uid
        }
    }

    // ========== Remember Me ==========

    /**
     * Save login credentials for "Remember Me" functionality
     */
    suspend fun saveCredentials(username: String, password: String, rememberMe: Boolean) {
        dataStore.edit { prefs ->
            prefs[REMEMBER_ME] = rememberMe
            if (rememberMe) {
                prefs[SAVED_USERNAME] = username
                prefs[SAVED_PASSWORD] = password
            } else {
                prefs.remove(SAVED_USERNAME)
                prefs.remove(SAVED_PASSWORD)
            }
        }
    }

    /**
     * Check if "Remember Me" is enabled
     */
    fun isRememberMeEnabled(): Flow<Boolean> {
        return dataStore.data.map { prefs ->
            prefs[REMEMBER_ME] ?: false
        }
    }

    /**
     * Get saved username
     */
    fun getSavedUsername(): Flow<String?> {
        return dataStore.data.map { prefs ->
            prefs[SAVED_USERNAME]
        }
    }

    /**
     * Get saved password
     */
    fun getSavedPassword(): Flow<String?> {
        return dataStore.data.map { prefs ->
            prefs[SAVED_PASSWORD]
        }
    }

    /**
     * Get saved credentials synchronously (for initialization)
     */
    suspend fun getSavedCredentialsSync(): Pair<String?, String?> {
        val prefs = dataStore.data.first()
        val rememberMe = prefs[REMEMBER_ME] ?: false
        return if (rememberMe) {
            Pair(prefs[SAVED_USERNAME], prefs[SAVED_PASSWORD])
        } else {
            Pair(null, null)
        }
    }

    /**
     * Clear remember me credentials
     */
    suspend fun clearRememberMe() {
        dataStore.edit { prefs ->
            prefs.remove(REMEMBER_ME)
            prefs.remove(SAVED_USERNAME)
            prefs.remove(SAVED_PASSWORD)
        }
    }

    // ========== Clear All ==========

    suspend fun clearAll() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}