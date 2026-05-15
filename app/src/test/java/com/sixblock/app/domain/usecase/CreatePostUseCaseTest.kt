package com.sixblock.app.domain.usecase

import com.sixblock.app.core.model.Resource
import com.sixblock.app.domain.model.GeoPoint
import com.sixblock.app.domain.model.PostCategory
import com.sixblock.app.domain.model.PostDraft
import com.sixblock.app.domain.repository.CreatePostRequest
import com.sixblock.app.domain.repository.PostRepository
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatePostUseCaseTest {
    @Test
    fun shortTitle_returnsValidationError() = runTest {
        val useCase = CreatePostUseCase(FakePostRepository())

        val result = useCase(
            title = "Hi",
            body = "This has enough detail for validation.",
            category = PostCategory.HELP_REQUEST,
            location = GeoPoint(43.65, -79.38),
            approximateArea = "Toronto"
        )

        assertTrue(result is Resource.Error)
    }

    @Test
    fun validPost_createsPost() = runTest {
        val useCase = CreatePostUseCase(FakePostRepository())

        val result = useCase(
            title = "Free moving boxes",
            body = "Clean moving boxes available near Queen West.",
            category = PostCategory.FREE_STUFF,
            location = GeoPoint(43.65, -79.38),
            approximateArea = "Toronto"
        )

        assertTrue(result is Resource.Success)
    }

    private class FakePostRepository : PostRepository {
        override fun observeNearbyPosts(origin: GeoPoint, radiusKm: Int, category: PostCategory?) = emptyFlow<Resource<List<com.sixblock.app.domain.model.CommunityPost>>>()
        override fun observeSavedPosts(origin: GeoPoint?) = emptyFlow<Resource<List<com.sixblock.app.domain.model.CommunityPost>>>()
        override fun observeUserPosts(userId: String, origin: GeoPoint?) = emptyFlow<Resource<List<com.sixblock.app.domain.model.CommunityPost>>>()
        override suspend fun getPost(postId: String, origin: GeoPoint?) = Resource.Error("No fake post")
        override suspend fun createPost(request: CreatePostRequest) = Resource.Success("post-1")
        override suspend fun editPost(postId: String, title: String, body: String, category: PostCategory?) = Resource.Success(Unit)
        override suspend fun deletePost(postId: String) = Resource.Success(Unit)
        override suspend fun togglePostLike(postId: String, currentlyLiked: Boolean) = Resource.Success(Unit)
        override suspend fun togglePostSave(postId: String, currentlySaved: Boolean) = Resource.Success(Unit)
        override suspend fun saveDraft(draft: PostDraft) = Unit
        override suspend fun getLatestDraft(): PostDraft? = null
        override suspend fun getDrafts(): List<PostDraft> = emptyList()
        override suspend fun deleteDraft(draftId: String) = Unit
    }
}
