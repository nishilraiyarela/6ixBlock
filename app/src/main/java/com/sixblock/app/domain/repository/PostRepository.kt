package com.sixblock.app.domain.repository

import com.sixblock.app.core.model.Resource
import com.sixblock.app.domain.model.CommunityPost
import com.sixblock.app.domain.model.GeoPoint
import com.sixblock.app.domain.model.PostCategory
import com.sixblock.app.domain.model.PostDraft
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun observeNearbyPosts(
        origin: GeoPoint,
        radiusKm: Int,
        category: PostCategory? = null
    ): Flow<Resource<List<CommunityPost>>>

    fun observeSavedPosts(origin: GeoPoint? = null): Flow<Resource<List<CommunityPost>>>
    fun observeUserPosts(userId: String, origin: GeoPoint? = null): Flow<Resource<List<CommunityPost>>>
    suspend fun getPost(postId: String, origin: GeoPoint? = null): Resource<CommunityPost>
    suspend fun createPost(request: CreatePostRequest): Resource<String>
    suspend fun editPost(postId: String, title: String, body: String, category: PostCategory? = null): Resource<Unit>
    suspend fun togglePostLike(postId: String, currentlyLiked: Boolean): Resource<Unit>
    suspend fun togglePostSave(postId: String, currentlySaved: Boolean): Resource<Unit>
    suspend fun deletePost(postId: String): Resource<Unit>
    suspend fun saveDraft(draft: PostDraft)
    suspend fun getLatestDraft(): PostDraft?
    suspend fun getDrafts(): List<PostDraft>
    suspend fun deleteDraft(draftId: String)
}

data class CreatePostRequest(
    val title: String,
    val body: String,
    val category: PostCategory,
    val location: GeoPoint,
    val approximateArea: String,
    val imageUrl: String?
)
