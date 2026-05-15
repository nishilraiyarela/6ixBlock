package com.sixblock.app.domain.repository

import com.sixblock.app.core.model.Resource
import com.sixblock.app.domain.model.ReportTargetType

interface ReportRepository {
    suspend fun reportContent(targetId: String, targetType: ReportTargetType, reason: String): Resource<Unit>
    suspend fun hideContent(targetId: String, targetType: ReportTargetType)
}
