package com.anugraha.stays.domain.repository

import com.anugraha.stays.domain.model.Statement
import com.anugraha.stays.util.NetworkResult
import java.time.LocalDate

interface StatementRepository {
    suspend fun generateStatement(
        startDate: LocalDate,
        endDate: LocalDate
    ): NetworkResult<Statement>
}