package com.anugraha.stays.domain.usecase.statement

import com.anugraha.stays.domain.model.Statement
import com.anugraha.stays.domain.repository.StatementRepository
import com.anugraha.stays.util.NetworkResult
import java.time.LocalDate
import javax.inject.Inject

class GenerateStatementUseCase @Inject constructor(
    private val statementRepository: StatementRepository
) {
    suspend operator fun invoke(
        startDate: LocalDate,
        endDate: LocalDate
    ): NetworkResult<Statement> {
        return statementRepository.generateStatement(startDate, endDate)
    }
}