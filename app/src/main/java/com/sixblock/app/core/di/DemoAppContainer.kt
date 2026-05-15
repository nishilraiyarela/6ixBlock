package com.sixblock.app.core.di

import android.content.Context
import com.sixblock.app.core.model.Resource
import com.sixblock.app.core.util.GeoHash
import com.sixblock.app.core.util.GeoUtils
import com.sixblock.app.core.util.TorontoDefaults
import com.sixblock.app.domain.model.CommentStatus
import com.sixblock.app.domain.model.CommunityPost
import com.sixblock.app.domain.model.GeoPoint
import com.sixblock.app.domain.model.NotificationItem
import com.sixblock.app.domain.model.NotificationType
import com.sixblock.app.domain.model.PostCategory
import com.sixblock.app.domain.model.PostComment
import com.sixblock.app.domain.model.PostDraft
import com.sixblock.app.domain.model.PostStatus
import com.sixblock.app.domain.model.ReportTargetType
import com.sixblock.app.domain.model.UserProfile
import com.sixblock.app.domain.repository.AuthRepository
import com.sixblock.app.domain.repository.CommentRepository
import com.sixblock.app.domain.repository.CreatePostRequest
import com.sixblock.app.domain.repository.NotificationRepository
import com.sixblock.app.domain.repository.PostRepository
import com.sixblock.app.domain.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.UUID

class DemoAppContainer(context: Context) : AppContainer(context) {
    private val demoStore = DemoStore()

    override val authRepository: AuthRepository = DemoAuthRepository(demoStore)
    override val postRepository: PostRepository = DemoPostRepository(demoStore)
    override val commentRepository: CommentRepository = DemoCommentRepository(demoStore)
    override val reportRepository: ReportRepository = DemoReportRepository(demoStore)
    override val notificationRepository: NotificationRepository = DemoNotificationRepository(demoStore)
}

private class DemoStore {
    val currentUser = MutableStateFlow<UserProfile?>(null)
    val posts = MutableStateFlow(seedPosts())
    val comments = MutableStateFlow(
        mapOf(
            "demo-1" to listOf(
                PostComment(
                    id = "comment-1",
                    postId = "demo-1",
                    body = "I saw a similar dog near Trinity Bellwoods this morning.",
                    authorId = "demo-neighbour",
                    authorName = "Maya",
                    createdAt = System.currentTimeMillis() - 900_000,
                    status = CommentStatus.ACTIVE
                )
            )
        )
    )
    val notifications = MutableStateFlow(
        listOf(
            NotificationItem(
                id = "activity-1",
                title = "Welcome to 6ixBlock demo mode",
                body = "Add Firebase config to use live auth, posts, and notifications.",
                postId = null,
                type = NotificationType.SYSTEM,
                createdAt = System.currentTimeMillis(),
                read = false
            )
        )
    )
    val drafts = mutableListOf<PostDraft>()
    val hiddenPostIds = mutableSetOf<String>()
    val likedPostIds = mutableSetOf<String>()
    val savedPostIds = mutableSetOf<String>()

    private fun seedPosts(): List<CommunityPost> {
        val now = System.currentTimeMillis()
        return listOf(
            demoPost("demo-1", "Lost golden doodle near Queen West", "Friendly dog named Milo, blue collar. Last seen heading east around the park.", PostCategory.LOST_PET, GeoPoint(43.6468, -79.4124), now - 1_800_000, 1),
            demoPost("demo-2", "Free moving boxes", "Clean medium boxes by the lobby. Pickup near Liberty Village before 7 PM.", PostCategory.FREE_STUFF, GeoPoint(43.6387, -79.4197), now - 3_600_000, 0),
            demoPost("demo-3", "Weekend street cleanup", "Neighbours are meeting Saturday morning for a quick block cleanup and coffee after.", PostCategory.LOCAL_EVENT, GeoPoint(43.6544, -79.3807), now - 7_200_000, 3)
        )
    }

    private fun demoPost(
        id: String,
        title: String,
        body: String,
        category: PostCategory,
        point: GeoPoint,
        createdAt: Long,
        comments: Int
    ): CommunityPost = CommunityPost(
        id = id,
        title = title,
        body = body,
        category = category,
        authorId = "demo-user",
        authorName = "Demo Neighbour",
        location = point,
        geohash = GeoHash.encode(point),
        cityKey = TorontoDefaults.cityKey,
        approximateArea = GeoUtils.coarseArea(point),
        imageUrl = null,
        createdAt = createdAt,
        updatedAt = createdAt,
        commentCount = comments,
        reportCount = 0,
        status = PostStatus.ACTIVE,
        distanceKm = GeoUtils.distanceKm(TorontoDefaults.center, point)
    )
}

private class DemoAuthRepository(private val store: DemoStore) : AuthRepository {
    override val currentUser: Flow<UserProfile?> = store.currentUser

    override suspend fun signIn(email: String, password: String): Resource<UserProfile> {
        val profile = demoProfile(email = email.ifBlank { "demo@sixblock.local" })
        store.currentUser.value = profile
        return Resource.Success(profile)
    }

    override suspend fun signUp(
        displayName: String,
        email: String,
        password: String,
        birthday: String,
        neighbourhood: String,
        gender: String
    ): Resource<UserProfile> {
        val profile = demoProfile(
            displayName = displayName.ifBlank { "Demo Neighbour" },
            email = email.ifBlank { "demo@sixblock.local" },
            birthday = birthday,
            neighbourhood = neighbourhood,
            gender = gender
        )
        store.currentUser.value = profile
        return Resource.Success(profile)
    }

    override suspend fun signInWithGoogle(idToken: String): Resource<UserProfile> {
        val profile = demoProfile()
        store.currentUser.value = profile
        return Resource.Success(profile)
    }

    override suspend fun updateProfileInfo(email: String, photoUrl: String, neighbourhood: String, gender: String): Resource<UserProfile> {
        val updated = (store.currentUser.value ?: demoProfile()).copy(
            email = email.trim().ifBlank { store.currentUser.value?.email },
            photoUrl = photoUrl.trim().ifBlank { store.currentUser.value?.photoUrl },
            neighbourhood = neighbourhood.trim(),
            gender = gender.trim()
        )
        store.currentUser.value = updated
        return Resource.Success(updated)
    }

    override suspend fun registerFcmToken() = Unit
    override fun signOut() {
        store.currentUser.value = null
    }

    private fun demoProfile(
        displayName: String = "Demo Neighbour",
        email: String = "demo@sixblock.local",
        birthday: String = "",
        neighbourhood: String = "",
        gender: String = ""
    ): UserProfile = UserProfile(
        id = "demo-user",
        displayName = displayName,
        email = email,
        photoUrl = null,
        birthday = birthday,
        neighbourhood = neighbourhood,
        gender = gender
    )
}

private class DemoPostRepository(private val store: DemoStore) : PostRepository {
    override fun observeNearbyPosts(origin: GeoPoint, radiusKm: Int, category: PostCategory?): Flow<Resource<List<CommunityPost>>> {
        return store.posts.map { posts ->
            val visible = posts
                .filterNot { it.id in store.hiddenPostIds }
                .map {
                    it.copy(
                        distanceKm = GeoUtils.distanceKm(origin, it.location),
                        likedByCurrentUser = it.id in store.likedPostIds,
                        savedByCurrentUser = it.id in store.savedPostIds
                    )
                }
                .filter { it.distanceKm == null || it.distanceKm <= radiusKm }
                .filter { category == null || it.category == category }
            if (visible.isEmpty()) Resource.Empty("No posts nearby yet.") else Resource.Success(visible)
        }
    }

    override fun observeSavedPosts(origin: GeoPoint?): Flow<Resource<List<CommunityPost>>> {
        return store.posts.map { posts ->
            val saved = posts
                .filter { it.id in store.savedPostIds }
                .map {
                    it.copy(
                        distanceKm = origin?.let { userLocation -> GeoUtils.distanceKm(userLocation, it.location) },
                        likedByCurrentUser = it.id in store.likedPostIds,
                        savedByCurrentUser = true
                    )
                }
            if (saved.isEmpty()) Resource.Empty("No saved posts yet.") else Resource.Success(saved)
        }
    }

    override fun observeUserPosts(userId: String, origin: GeoPoint?): Flow<Resource<List<CommunityPost>>> {
        return store.posts.map { posts ->
            val mine = posts
                .filter { it.authorId == userId }
                .map {
                    it.copy(
                        distanceKm = origin?.let { userLocation -> GeoUtils.distanceKm(userLocation, it.location) },
                        likedByCurrentUser = it.id in store.likedPostIds,
                        savedByCurrentUser = it.id in store.savedPostIds
                    )
                }
            if (mine.isEmpty()) Resource.Empty("You haven't posted anything yet.") else Resource.Success(mine)
        }
    }

    override suspend fun getPost(postId: String, origin: GeoPoint?): Resource<CommunityPost> {
        val post = store.posts.value.firstOrNull { it.id == postId }
            ?: return Resource.Error("Post not found.")
        return Resource.Success(
            post.copy(
                distanceKm = origin?.let { GeoUtils.distanceKm(it, post.location) },
                likedByCurrentUser = post.id in store.likedPostIds,
                savedByCurrentUser = post.id in store.savedPostIds
            )
        )
    }

    override suspend fun createPost(request: CreatePostRequest): Resource<String> {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val post = CommunityPost(
            id = id,
            title = request.title,
            body = request.body,
            category = request.category,
            authorId = store.currentUser.value?.id ?: "demo-user",
            authorName = store.currentUser.value?.displayName ?: "Demo Neighbour",
            location = request.location,
            geohash = GeoHash.encode(request.location),
            cityKey = TorontoDefaults.cityKey,
            approximateArea = request.approximateArea,
            imageUrl = request.imageUrl,
            createdAt = now,
            updatedAt = now,
            commentCount = 0,
            reportCount = 0,
            status = PostStatus.ACTIVE,
            distanceKm = GeoUtils.distanceKm(TorontoDefaults.center, request.location)
        )
        store.posts.value = listOf(post) + store.posts.value
        return Resource.Success(id)
    }

    override suspend fun editPost(postId: String, title: String, body: String, category: PostCategory?): Resource<Unit> {
        store.posts.value = store.posts.value.map { post ->
            if (post.id == postId) {
                post.copy(
                    title = title.trim(),
                    body = body.trim(),
                    category = category ?: post.category,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                post
            }
        }
        return Resource.Success(Unit)
    }

    override suspend fun deletePost(postId: String): Resource<Unit> {
        store.posts.value = store.posts.value.filterNot { it.id == postId }
        return Resource.Success(Unit)
    }

    override suspend fun togglePostLike(postId: String, currentlyLiked: Boolean): Resource<Unit> {
        val liked = postId in store.likedPostIds
        if (currentlyLiked || liked) store.likedPostIds.remove(postId) else store.likedPostIds.add(postId)
        store.posts.value = store.posts.value.map { post ->
            if (post.id == postId) {
                val nextLiked = postId in store.likedPostIds
                val delta = if (nextLiked) 1 else -1
                post.copy(
                    likeCount = (post.likeCount + delta).coerceAtLeast(0),
                    likedByCurrentUser = nextLiked
                )
            } else {
                post
            }
        }
        return Resource.Success(Unit)
    }

    override suspend fun togglePostSave(postId: String, currentlySaved: Boolean): Resource<Unit> {
        val saved = postId in store.savedPostIds
        if (currentlySaved || saved) store.savedPostIds.remove(postId) else store.savedPostIds.add(postId)
        store.posts.value = store.posts.value.map { post ->
            if (post.id == postId) post.copy(savedByCurrentUser = postId in store.savedPostIds) else post
        }
        return Resource.Success(Unit)
    }

    override suspend fun saveDraft(draft: PostDraft) {
        store.drafts.removeAll { it.id == draft.id }
        store.drafts.add(0, draft)
    }

    override suspend fun getLatestDraft(): PostDraft? = store.drafts.firstOrNull()

    override suspend fun getDrafts(): List<PostDraft> = store.drafts.toList()

    override suspend fun deleteDraft(draftId: String) {
        store.drafts.removeAll { it.id == draftId }
    }
}

private class DemoCommentRepository(private val store: DemoStore) : CommentRepository {
    override fun observeComments(postId: String): Flow<Resource<List<PostComment>>> {
        return store.comments.map { comments ->
            Resource.Success<List<PostComment>>(comments[postId].orEmpty())
        }
    }

    override suspend fun addComment(postId: String, body: String): Resource<String> {
        val comment = PostComment(
            id = UUID.randomUUID().toString(),
            postId = postId,
            body = body,
            authorId = store.currentUser.value?.id ?: "demo-user",
            authorName = store.currentUser.value?.displayName ?: "Demo Neighbour",
            createdAt = System.currentTimeMillis()
        )
        val current = store.comments.value.toMutableMap()
        current[postId] = current[postId].orEmpty() + comment
        store.comments.value = current
        return Resource.Success(comment.id)
    }

    override suspend fun editComment(postId: String, commentId: String, body: String): Resource<Unit> {
        val current = store.comments.value.toMutableMap()
        val updated = current[postId].orEmpty().map { comment ->
            if (comment.id == commentId) {
                comment.copy(body = body.trim(), updatedAt = System.currentTimeMillis(), edited = true)
            } else {
                comment
            }
        }
        current[postId] = updated
        store.comments.value = current
        return Resource.Success(Unit)
    }

    override suspend fun deleteComment(postId: String, commentId: String): Resource<Unit> {
        val current = store.comments.value.toMutableMap()
        current[postId] = current[postId].orEmpty().filterNot { it.id == commentId }
        store.comments.value = current
        store.posts.value = store.posts.value.map { post ->
            if (post.id == postId) post.copy(commentCount = (post.commentCount - 1).coerceAtLeast(0)) else post
        }
        return Resource.Success(Unit)
    }
}

private class DemoReportRepository(private val store: DemoStore) : ReportRepository {
    override suspend fun reportContent(targetId: String, targetType: ReportTargetType, reason: String): Resource<Unit> {
        return Resource.Success(Unit)
    }

    override suspend fun hideContent(targetId: String, targetType: ReportTargetType) {
        if (targetType == ReportTargetType.POST) store.hiddenPostIds.add(targetId)
    }
}

private class DemoNotificationRepository(private val store: DemoStore) : NotificationRepository {
    override fun observeNotifications(): Flow<Resource<List<NotificationItem>>> =
        flowOf(Resource.Success<List<NotificationItem>>(store.notifications.value))
    override suspend fun markRead(notificationId: String) = Unit
    override suspend fun markAllRead() = Unit
    override suspend fun setNotificationPreference(key: String, enabled: Boolean) = Unit
}
