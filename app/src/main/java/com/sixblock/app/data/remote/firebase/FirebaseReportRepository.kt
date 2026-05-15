package com.sixblock.app.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.sixblock.app.core.model.Resource
import com.sixblock.app.data.local.dao.HiddenContentDao
import com.sixblock.app.data.local.entity.HiddenContentEntity
import com.sixblock.app.domain.model.ReportTargetType
import com.sixblock.app.domain.repository.ReportRepository
import kotlinx.coroutines.tasks.await

class FirebaseReportRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val hiddenContentDao: HiddenContentDao
) : ReportRepository {

    override suspend fun reportContent(
        targetId: String,
        targetType: ReportTargetType,
        reason: String
    ): Resource<Unit> {
        val user = auth.currentUser ?: return Resource.Error("Sign in before reporting content.")
        return runCatching {
            val reportRef = firestore.collection("reports").document()
            reportRef.set(
                mapOf(
                    "id" to reportRef.id,
                    "targetId" to targetId,
                    "targetType" to targetType.id,
                    "reason" to reason,
                    "reporterId" to user.uid,
                    "createdAt" to System.currentTimeMillis()
                )
            ).await()
            if (targetType == ReportTargetType.POST) {
                firestore.collection("posts")
                    .document(targetId)
                    .update("reportCount", FieldValue.increment(1))
                    .await()
            }
            Resource.Success(Unit)
        }.getOrElse { Resource.Error(it.message ?: "Unable to submit report.", it) }
    }

    override suspend fun hideContent(targetId: String, targetType: ReportTargetType) {
        hiddenContentDao.hide(HiddenContentEntity(targetId, targetType.id))
    }
}
