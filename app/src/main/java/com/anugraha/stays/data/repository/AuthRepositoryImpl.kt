package com.anugraha.stays.data.repository

import com.anugraha.stays.data.local.preferences.UserPreferences
import com.anugraha.stays.data.remote.firebase.FirebaseAuthDataSource
import com.anugraha.stays.domain.model.User
import com.anugraha.stays.domain.repository.AuthRepository
import com.anugraha.stays.util.NetworkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuthDataSource: FirebaseAuthDataSource,
    private val userPreferences: UserPreferences
) : AuthRepository {

    override suspend fun login(email: String, password: String): NetworkResult<User> {
        val result = firebaseAuthDataSource.login(email, password)

        if (result is NetworkResult.Success) {
            userPreferences.saveUserEmail(result.data.email)
            userPreferences.saveUserUid(result.data.uid)
            userPreferences.saveAuthToken(result.data.uid)
        }

        return result
    }

    override suspend fun logout(): NetworkResult<Unit> {
        val result = firebaseAuthDataSource.logout()
        if (result is NetworkResult.Success) {
            userPreferences.clearAll()
        }
        return result
    }

    override fun getCurrentUser(): Flow<User?> = flow {
        emit(firebaseAuthDataSource.getCurrentUser())
    }

    override suspend fun isUserLoggedIn(): Boolean {
        return firebaseAuthDataSource.isUserLoggedIn()
    }
}