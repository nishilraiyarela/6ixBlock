package com.sixblock.app.domain.usecase

import com.sixblock.app.domain.model.GeoPoint
import com.sixblock.app.domain.model.PostCategory
import com.sixblock.app.domain.repository.PostRepository

class ObserveNearbyPostsUseCase(private val postRepository: PostRepository) {
    operator fun invoke(origin: GeoPoint, radiusKm: Int, category: PostCategory?) =
        postRepository.observeNearbyPosts(origin, radiusKm, category)
}
