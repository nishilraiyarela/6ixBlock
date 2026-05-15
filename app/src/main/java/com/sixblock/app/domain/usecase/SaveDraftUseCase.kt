package com.sixblock.app.domain.usecase

import com.sixblock.app.domain.model.GeoPoint
import com.sixblock.app.domain.model.PostCategory
import com.sixblock.app.domain.model.PostDraft
import com.sixblock.app.domain.repository.PostRepository
import java.util.UUID

class SaveDraftUseCase(private val postRepository: PostRepository) {
    suspend operator fun invoke(
        title: String,
        body: String,
        category: PostCategory,
        localPhotoUri: String?,
        location: GeoPoint,
        approximateArea: String
    ) {
        postRepository.saveDraft(
            PostDraft(
                id = UUID.randomUUID().toString(),
                title = title,
                body = body,
                category = category,
                localPhotoUri = localPhotoUri,
                location = location,
                approximateArea = approximateArea,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
