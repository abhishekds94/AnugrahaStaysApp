package com.anugraha.stays.domain.usecase.auth

import com.anugraha.stays.domain.repository.AuthRepository
import com.anugraha.stays.util.NetworkResult
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): NetworkResult<Unit> {
        return authRepository.logout()
    }
}