package com.sixblock.app.domain.usecase

import com.sixblock.app.core.model.Resource
import com.sixblock.app.domain.model.GeoPoint
import com.sixblock.app.domain.model.PostCategory
import com.sixblock.app.domain.repository.CreatePostRequest
import com.sixblock.app.domain.repository.PostRepository

class CreatePostUseCase(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(
        title: String,
        body: String,
        category: PostCategory,
        location: GeoPoint,
        approximateArea: String
    ): Resource<String> {
        val cleanTitle = title.trim()
        val cleanBody = body.trim()
        if (cleanTitle.length < 4) return Resource.Error("Add a clear title.")
        if (cleanBody.length < 12) return Resource.Error("Add a few more details.")

        return postRepository.createPost(
            CreatePostRequest(
                title = cleanTitle,
                body = cleanBody,
                category = category,
                location = location,
                approximateArea = approximateArea,
                imageUrl = null
            )
        )
    }
}
