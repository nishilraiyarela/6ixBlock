package com.sixblock.app.domain.usecase

import com.sixblock.app.core.model.Resource
import com.sixblock.app.domain.model.ReportTargetType
import com.sixblock.app.domain.repository.ReportRepository

class ReportContentUseCase(private val reportRepository: ReportRepository) {
    suspend operator fun invoke(
        targetId: String,
        targetType: ReportTargetType,
        reason: String
    ): Resource<Unit> {
        val cleanReason = reason.trim().ifBlank { "Community safety report" }
        return reportRepository.reportContent(targetId, targetType, cleanReason)
    }
}
