package com.anugraha.stays.domain.repository

import com.anugraha.stays.domain.model.User
import com.anugraha.stays.util.NetworkResult
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(email: String, password: String): NetworkResult<User>
    suspend fun logout(): NetworkResult<Unit>
    fun getCurrentUser(): Flow<User?>
    suspend fun isUserLoggedIn(): Boolean
}