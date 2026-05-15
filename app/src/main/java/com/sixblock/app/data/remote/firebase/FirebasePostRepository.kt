package com.sixblock.app.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.sixblock.app.core.model.Resource
import com.sixblock.app.core.util.GeoHash
import com.sixblock.app.core.util.GeoUtils
import com.sixblock.app.core.util.TorontoDefaults
import com.sixblock.app.data.local.dao.DraftDao
import com.sixblock.app.data.local.dao.HiddenContentDao
import com.sixblock.app.data.local.dao.PostDao
import com.sixblock.app.data.mapper.toDomain
import com.sixblock.app.data.mapper.toEntity
import com.sixblock.app.domain.model.CommentStatus
import com.sixblock.app.domain.model.CommunityPost
import com.sixblock.app.domain.model.GeoPoint
import com.sixblock.app.domain.model.PostCategory
import com.sixblock.app.domain.model.PostDraft
import com.sixblock.app.domain.model.PostStatus
import com.sixblock.app.domain.repository.CreatePostRequest
import com.sixblock.app.domain.repository.PostRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirebasePostRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val postDao: PostDao,
    private val draftDao: DraftDao,
    private val hiddenContentDao: HiddenContentDao
) : PostRepository {

    override fun observeNearbyPosts(
        origin: GeoPoint,
        radiusKm: Int,
        category: PostCategory?
    ): Flow<Resource<List<CommunityPost>>> = callbackFlow {
        trySend(Resource.Loading)
        val currentUserId = auth.currentUser?.uid
        var livePosts = emptyList<CommunityPost>()
        val childRegistrations = mutableMapOf<String, MutableList<ListenerRegistration>>()

        fun emitLivePosts() {
            val posts = livePosts.sortedByDescending { it.createdAt }
            if (posts.isEmpty()) trySend(Resource.Empty("No posts nearby yet."))
            else trySend(Resource.Success(posts))
        }

        fun updateLivePost(postId: String, transform: (CommunityPost) -> CommunityPost) {
            livePosts = livePosts.map { post -> if (post.id == postId) transform(post) else post }
            emitLivePosts()
        }

        fun attachChildListeners(posts: List<CommunityPost>) {
            val activeIds = posts.map { it.id }.toSet()
            childRegistrations
                .filterKeys { it !in activeIds }
                .toList()
                .forEach { (postId, registrations) ->
                    registrations.forEach { it.remove() }
                    childRegistrations.remove(postId)
                }

            posts.forEach { post ->
                if (childRegistrations.containsKey(post.id)) return@forEach
                val postRef = firestore.collection("posts").document(post.id)
                val registrations = mutableListOf<ListenerRegistration>()

                registrations += postRef.collection("likes")
                    .addSnapshotListener { snapshot, _ ->
                        val documents = snapshot?.documents.orEmpty()
                        val likedByViewer = currentUserId != null && documents.any { it.id == currentUserId }
                        updateLivePost(post.id) {
                            it.copy(
                                likeCount = documents.size.coerceAtLeast(0),
                                likedByCurrentUser = likedByViewer
                            )
                        }
                    }

                registrations += postRef.collection("comments")
                    .addSnapshotListener { snapshot, _ ->
                        val activeCount = snapshot?.documents.orEmpty()
                            .count { CommentStatus.fromId(it.getString("status")) == CommentStatus.ACTIVE }
                        updateLivePost(post.id) { it.copy(commentCount = activeCount.coerceAtLeast(0)) }
                    }

                childRegistrations[post.id] = registrations
            }
        }

        val hidden = hiddenContentDao.hiddenIds("post").toSet()
        val cached = postDao.getRecentPosts()
            .asSequence()
            .filterNot { it.id in hidden }
            .map { it.toDomain(origin) }
            .filter { it.status == PostStatus.ACTIVE }
            .filter { it.distanceKm == null || it.distanceKm <= radiusKm }
            .filter { category == null || it.category == category }
            .toList()
        if (cached.isNotEmpty()) trySend(Resource.Success(cached, fromCache = true))

        val registration = firestore.collection("posts")
            .whereEqualTo("cityKey", TorontoDefaults.cityKey)
            .limit(250)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Unable to load nearby posts.", error))
                    return@addSnapshotListener
                }

                val posts = snapshot?.documents
                    .orEmpty()
                    .mapNotNull { it.toCommunityPost(origin) }
                    .filterNot { it.id in hidden }
                    .filter { it.status == PostStatus.ACTIVE }
                    .filter { it.distanceKm == null || it.distanceKm <= radiusKm }
                    .filter { category == null || it.category == category }
                    .sortedByDescending { it.createdAt }

                launch {
                    val postsWithViewerState = attachViewerState(posts)
                    livePosts = postsWithViewerState
                    postDao.upsertPosts(postsWithViewerState.map { it.toEntity() })
                    attachChildListeners(postsWithViewerState)
                    emitLivePosts()
                }
            }

        awaitClose {
            registration.remove()
            childRegistrations.values.flatten().forEach { it.remove() }
        }
    }

    override fun observeSavedPosts(origin: GeoPoint?): Flow<Resource<List<CommunityPost>>> = callbackFlow {
        val user = auth.currentUser
        if (user == null) {
            trySend(Resource.Empty("Sign in to see saved posts."))
            close()
            return@callbackFlow
        }

        trySend(Resource.Loading)
        val registration = firestore.collection("users")
            .document(user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Unable to load saved posts.", error))
                    return@addSnapshotListener
                }

                val savedIds = (snapshot?.get("savedPostIds") as? List<*>)
                    .orEmpty()
                    .filterIsInstance<String>()
                    .asReversed()

                launch {
                    val posts = savedIds.mapNotNull { postId ->
                        runCatching {
                            firestore.collection("posts")
                                .document(postId)
                                .get()
                                .await()
                                .toCommunityPost(origin)
                        }.getOrNull()
                    }.filter { it.status == PostStatus.ACTIVE }
                        .map { it.copy(savedByCurrentUser = true) }

                    if (posts.isEmpty()) trySend(Resource.Empty("No saved posts yet."))
                    else trySend(Resource.Success(attachViewerState(posts)))
                }
            }

        awaitClose { registration.remove() }
    }

    override fun observeUserPosts(userId: String, origin: GeoPoint?): Flow<Resource<List<CommunityPost>>> = callbackFlow {
        trySend(Resource.Loading)

        val cached = postDao.getRecentPosts()
            .asSequence()
            .map { it.toDomain(origin) }
            .filter { it.authorId == userId }
            .filter { it.status == PostStatus.ACTIVE }
            .sortedByDescending { it.createdAt }
            .toList()
        if (cached.isNotEmpty()) {
            trySend(Resource.Success(cached, fromCache = true))
        }

        val registration = firestore.collection("posts")
            .whereEqualTo("cityKey", TorontoDefaults.cityKey)
            .limit(250)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Unable to load your posts.", error))
                    return@addSnapshotListener
                }

                val posts = snapshot?.documents
                    .orEmpty()
                    .mapNotNull { it.toCommunityPost(origin) }
                    .filter { it.authorId == userId }
                    .filter { it.status == PostStatus.ACTIVE }
                    .sortedByDescending { it.createdAt }

                launch {
                    val postsWithViewerState = attachViewerState(posts)
                    postDao.upsertPosts(postsWithViewerState.map { it.toEntity() })
                    if (postsWithViewerState.isEmpty()) {
                        trySend(Resource.Empty("You haven't posted anything yet."))
                    } else {
                        trySend(Resource.Success(postsWithViewerState))
                    }
                }
            }

        awaitClose { registration.remove() }
    }

    override suspend fun getPost(postId: String, origin: GeoPoint?): Resource<CommunityPost> {
        return runCatching {
            val snapshot = firestore.collection("posts").document(postId).get().await()
            val remote = snapshot.toCommunityPost(origin)
            if (remote != null) {
                val post = attachViewerState(listOf(remote)).first()
                postDao.upsertPosts(listOf(post.toEntity()))
                Resource.Success(post)
            } else {
                val cached = postDao.getPost(postId)?.toDomain(origin)
                if (cached != null) Resource.Success(cached, fromCache = true)
                else Resource.Error("Post not found.")
            }
        }.getOrElse { error ->
            val cached = postDao.getPost(postId)?.toDomain(origin)
            if (cached != null) Resource.Success(cached, fromCache = true)
            else Resource.Error(error.message ?: "Unable to load post.", error)
        }
    }

    override suspend fun createPost(request: CreatePostRequest): Resource<String> {
        val user = auth.currentUser ?: return Resource.Error("Sign in before posting.")
        return runCatching {
            val doc = firestore.collection("posts").document()
            val now = System.currentTimeMillis()
            val authorName = user.displayName?.takeIf { it.isNotBlank() }
                ?: user.email?.substringBefore("@")
                ?: "Neighbour"
            val geohash = GeoHash.encode(request.location)
            val post = mapOf(
                "id" to doc.id,
                "title" to request.title,
                "body" to request.body,
                "categoryId" to request.category.id,
                "authorId" to user.uid,
                "authorName" to authorName,
                "latitude" to request.location.latitude,
                "longitude" to request.location.longitude,
                "geohash" to geohash,
                "cityKey" to TorontoDefaults.cityKey,
                "approximateArea" to request.approximateArea,
                "imageUrl" to request.imageUrl,
                "createdAt" to now,
                "updatedAt" to now,
                "commentCount" to 0,
                "likeCount" to 0,
                "reportCount" to 0,
                "status" to PostStatus.ACTIVE.id,
                "urgent" to request.category.isUrgent
            )
            doc.set(post).await()
            Resource.Success(doc.id)
        }.getOrElse { Resource.Error(it.message ?: "Unable to publish post.", it) }
    }

    override suspend fun editPost(postId: String, title: String, body: String, category: PostCategory?): Resource<Unit> {
        val user = auth.currentUser ?: return Resource.Error("Sign in before editing.")
        val cleanTitle = title.trim()
        val cleanBody = body.trim()
        if (cleanTitle.length < 4) return Resource.Error("Add a clear title.")
        if (cleanBody.length < 12) return Resource.Error("Add a few more details.")

        return runCatching {
            val postRef = firestore.collection("posts").document(postId)
            val snapshot = postRef.get().await()
            if (snapshot.postOwnerId() != user.uid) return Resource.Error("You can only edit your own post.")
            val updates = mutableMapOf<String, Any>(
                "title" to cleanTitle,
                "body" to cleanBody,
                "updatedAt" to System.currentTimeMillis()
            )
            category?.let { updates["categoryId"] = it.id }
            postRef.update(updates).await()
            Resource.Success(Unit)
        }.getOrElse { Resource.Error(it.message ?: "Unable to edit post.", it) }
    }

    override suspend fun deletePost(postId: String): Resource<Unit> {
        val user = auth.currentUser ?: return Resource.Error("Sign in before deleting.")
        return runCatching {
            val postRef = firestore.collection("posts").document(postId)
            val snapshot = postRef.get().await()
            if (snapshot.postOwnerId() != user.uid) return Resource.Error("You can only delete your own post.")
            postRef.update(
                mapOf(
                    "status" to PostStatus.DELETED.id,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            postDao.deletePost(postId)
            Resource.Success(Unit)
        }.getOrElse { Resource.Error(it.message ?: "Unable to delete post.", it) }
    }

    override suspend fun togglePostLike(postId: String, currentlyLiked: Boolean): Resource<Unit> {
        val user = auth.currentUser ?: return Resource.Error("Sign in before liking posts.")
        val postRef = firestore.collection("posts").document(postId)
        val likeRef = postRef.collection("likes").document(user.uid)
        val userRef = firestore.collection("users").document(user.uid)
        val now = System.currentTimeMillis()
        val result = runCatching {
            firestore.runTransaction { transaction ->
                val likeSnapshot = transaction.get(likeRef)
                val isLiked = likeSnapshot.exists()
                val nextLiked = !isLiked
                val delta = if (nextLiked) 1L else -1L
                val postSnapshot = transaction.get(postRef)
                val currentCount = postSnapshot.getLong("likeCount") ?: 0L
                val nextCount = (currentCount + delta).coerceAtLeast(0L)

                if (nextLiked) {
                    transaction.set(
                        likeRef,
                        mapOf(
                            "userId" to user.uid,
                            "postId" to postId,
                            "createdAt" to now
                        )
                    )
                } else {
                    transaction.delete(likeRef)
                }
                transaction.update(
                    postRef,
                    mapOf(
                        "likeCount" to nextCount,
                        "updatedAt" to now
                    )
                )
                nextLiked
            }.await()
        }

        val nextLiked = result.getOrElse { error ->
            return Resource.Error(friendlyWriteMessage(error, "Unable to update like."), error)
        }

        runCatching {
            userRef.set(
                mapOf(
                    "likedPostIds" to if (nextLiked) {
                        FieldValue.arrayUnion(postId)
                    } else {
                        FieldValue.arrayRemove(postId)
                    }
                ),
                SetOptions.merge()
            ).await()
        }
        if (nextLiked) {
            createLikeNotification(postId, user.uid)
        }
        return Resource.Success(Unit)
    }

    override suspend fun togglePostSave(postId: String, currentlySaved: Boolean): Resource<Unit> {
        val user = auth.currentUser ?: return Resource.Error("Sign in before saving posts.")
        val userRef = firestore.collection("users").document(user.uid)
        val savedRef = userRef.collection("savedPosts").document(postId)
        val subcollectionResult = runCatching {
            if (currentlySaved) savedRef.delete().await()
            else {
                savedRef.set(mapOf("postId" to postId, "createdAt" to System.currentTimeMillis())).await()
            }
        }
        val profileResult = runCatching {
            userRef.set(
                mapOf(
                    "savedPostIds" to if (currentlySaved) {
                        FieldValue.arrayRemove(postId)
                    } else {
                        FieldValue.arrayUnion(postId)
                    }
                ),
                SetOptions.merge()
            ).await()
        }
        return if (subcollectionResult.isSuccess || profileResult.isSuccess) {
            Resource.Success(Unit)
        } else {
            val error = subcollectionResult.exceptionOrNull() ?: profileResult.exceptionOrNull()
            Resource.Error(error?.let { friendlyWriteMessage(it, "Unable to update saved post.") } ?: "Unable to update saved post.", error)
        }
    }

    private suspend fun attachViewerState(posts: List<CommunityPost>): List<CommunityPost> {
        val userId = auth.currentUser?.uid ?: return posts
        return posts.map { post ->
            val liked = runCatching {
                firestore.collection("posts")
                    .document(post.id)
                    .collection("likes")
                    .document(userId)
                    .get()
                    .await()
                    .exists()
            }.getOrDefault(false)
            val userLikedIds = userPostIds(userId, "likedPostIds")
            val saved = runCatching {
                firestore.collection("users")
                    .document(userId)
                    .collection("savedPosts")
                    .document(post.id)
                    .get()
                    .await()
                    .exists()
            }.getOrDefault(false)
            val userSavedIds = userPostIds(userId, "savedPostIds")
            val liveCommentCount = runCatching {
                firestore.collection("posts")
                    .document(post.id)
                    .collection("comments")
                    .whereEqualTo("status", CommentStatus.ACTIVE.id)
                    .get()
                    .await()
                    .size()
            }.getOrDefault(post.commentCount)
            post.copy(
                likedByCurrentUser = liked || post.id in userLikedIds,
                savedByCurrentUser = saved || post.id in userSavedIds,
                commentCount = liveCommentCount,
                likeCount = post.likeCount.coerceAtLeast(0)
            )
        }
    }

    private suspend fun userPostIds(userId: String, field: String): Set<String> {
        return runCatching {
            firestore.collection("users")
                .document(userId)
                .get()
                .await()
                .get(field) as? List<*>
        }.getOrNull()
            .orEmpty()
            .filterIsInstance<String>()
            .toSet()
    }

    private suspend fun createLikeNotification(postId: String, actorId: String) {
        runCatching {
            val postSnapshot = firestore.collection("posts").document(postId).get().await()
            val ownerId = postSnapshot.postOwnerId()
            val user = auth.currentUser
            if (ownerId.isNotBlank() && ownerId != actorId && user != null) {
                val ownerSnapshot = firestore.collection("users").document(ownerId).get().await()
                if (ownerSnapshot.getBoolean("likeNotificationsEnabled") != false) {
                    val actorName = user.displayName?.takeIf { it.isNotBlank() }
                        ?: user.email?.substringBefore("@")
                        ?: "Someone"
                    val postTitle = postSnapshot.getString("title").orEmpty()
                    val notificationRef = firestore.collection("users")
                        .document(ownerId)
                        .collection("notifications")
                        .document()
                    notificationRef.set(
                        mapOf(
                            "id" to notificationRef.id,
                            "title" to "New like",
                            "body" to "$actorName liked ${postTitle.ifBlank { "your post" }}",
                            "message" to "$actorName liked ${postTitle.ifBlank { "your post" }}",
                            "postId" to postId,
                            "type" to "like",
                            "actorId" to actorId,
                            "senderId" to actorId,
                            "recipientId" to ownerId,
                            "createdAt" to System.currentTimeMillis(),
                            "read" to false
                        )
                    ).await()
                }
            }
        }
    }

    private fun friendlyWriteMessage(error: Throwable, fallback: String): String {
        val message = error.message.orEmpty()
        return if (message.contains("PERMISSION_DENIED", ignoreCase = true) ||
            message.contains("permission", ignoreCase = true)
        ) {
            "Something went wrong. Please try again."
        } else {
            message.ifBlank { fallback }
        }
    }

    override suspend fun saveDraft(draft: PostDraft) {
        draftDao.upsertDraft(draft.toEntity())
    }

    override suspend fun getLatestDraft(): PostDraft? = draftDao.getLatestDraft()?.toDomain()

    override suspend fun getDrafts(): List<PostDraft> = draftDao.getDrafts().map { it.toDomain() }

    override suspend fun deleteDraft(draftId: String) {
        draftDao.deleteDraft(draftId)
    }
}
