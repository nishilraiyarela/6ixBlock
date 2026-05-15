package com.sixblock.app.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sixblock.app.core.model.Resource
import com.sixblock.app.data.local.dao.CommentDao
import com.sixblock.app.data.mapper.toDomain
import com.sixblock.app.data.mapper.toEntity
import com.sixblock.app.domain.model.CommentStatus
import com.sixblock.app.domain.model.PostComment
import com.sixblock.app.domain.repository.CommentRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirebaseCommentRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val commentDao: CommentDao
) : CommentRepository {

    override fun observeComments(postId: String): Flow<Resource<List<PostComment>>> = callbackFlow {
        trySend(Resource.Loading)
        val cached = commentDao.getComments(postId).map { it.toDomain() }
        if (cached.isNotEmpty()) trySend(Resource.Success(cached, fromCache = true))

        val registration = firestore.collection("posts")
            .document(postId)
            .collection("comments")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Unable to load comments.", error))
                    return@addSnapshotListener
                }
                val comments = snapshot?.documents.orEmpty()
                    .map { it.toPostComment(postId) }
                    .filter { it.status == CommentStatus.ACTIVE }
                    .sortedBy { it.createdAt }
                launch { commentDao.upsertComments(comments.map { it.toEntity() }) }
                trySend(Resource.Success(comments))
            }

        awaitClose { registration.remove() }
    }

    override suspend fun addComment(postId: String, body: String): Resource<String> {
        val user = auth.currentUser ?: return Resource.Error("Sign in before commenting.")
        return runCatching {
            val postRef = firestore.collection("posts").document(postId)
            val commentRef = postRef.collection("comments").document()
            val now = System.currentTimeMillis()
            val authorName = user.displayName?.takeIf { it.isNotBlank() }
                ?: user.email?.substringBefore("@")
                ?: "Neighbour"
            val postSnapshot = postRef.get().await()
            val postAuthorId = postSnapshot.postOwnerId()
            val postTitle = postSnapshot.getString("title").orEmpty()

            commentRef.set(
                mapOf(
                    "id" to commentRef.id,
                    "postId" to postId,
                    "body" to body,
                    "authorId" to user.uid,
                    "authorName" to authorName,
                    "createdAt" to now,
                    "updatedAt" to now,
                    "edited" to false,
                    "reportCount" to 0,
                    "status" to CommentStatus.ACTIVE.id
                )
            ).await()
            runCatching {
                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(postRef)
                    val currentCount = snapshot.getLong("commentCount") ?: 0L
                    transaction.update(
                        postRef,
                        mapOf(
                            "commentCount" to (currentCount + 1L).coerceAtLeast(0L),
                            "updatedAt" to now
                        )
                    )
                }.await()
            }
            if (postAuthorId.isNotBlank() && postAuthorId != user.uid) {
                createCommentNotification(
                    ownerId = postAuthorId,
                    actorId = user.uid,
                    actorName = authorName,
                    postId = postId,
                    postTitle = postTitle,
                    createdAt = now
                )
            }
            Resource.Success(commentRef.id)
        }.getOrElse { Resource.Error(friendlyWriteMessage(it, "Unable to add comment."), it) }
    }

    override suspend fun editComment(postId: String, commentId: String, body: String): Resource<Unit> {
        val user = auth.currentUser ?: return Resource.Error("Sign in before editing.")
        val cleanBody = body.trim()
        if (cleanBody.length < 2) return Resource.Error("Comment is too short.")

        return runCatching {
            val commentRef = firestore.collection("posts")
                .document(postId)
                .collection("comments")
                .document(commentId)
            val snapshot = commentRef.get().await()
            if (snapshot.getString("authorId") != user.uid) return Resource.Error("You can only edit your own comment.")
            if (CommentStatus.fromId(snapshot.getString("status")) != CommentStatus.ACTIVE) {
                return Resource.Error("This comment can no longer be edited.")
            }
            commentRef.update(
                mapOf(
                    "body" to cleanBody,
                    "updatedAt" to System.currentTimeMillis(),
                    "edited" to true
                )
            ).await()
            Resource.Success(Unit)
        }.getOrElse { Resource.Error(friendlyWriteMessage(it, "Unable to edit comment."), it) }
    }

    override suspend fun deleteComment(postId: String, commentId: String): Resource<Unit> {
        val user = auth.currentUser ?: return Resource.Error("Sign in before deleting.")
        return runCatching {
            val postRef = firestore.collection("posts").document(postId)
            val commentRef = postRef.collection("comments").document(commentId)
            val postSnapshot = postRef.get().await()
            val commentSnapshot = commentRef.get().await()
            val isCommentAuthor = commentSnapshot.getString("authorId") == user.uid
            val isPostOwner = postSnapshot.postOwnerId() == user.uid
            if (!isCommentAuthor && !isPostOwner) return Resource.Error("You cannot delete this comment.")
            if (CommentStatus.fromId(commentSnapshot.getString("status")) != CommentStatus.ACTIVE) {
                return Resource.Success(Unit)
            }
            val now = System.currentTimeMillis()
            commentRef.update(
                mapOf(
                    "body" to "",
                    "status" to CommentStatus.DELETED.id,
                    "updatedAt" to now
                )
            ).await()
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(postRef)
                val currentCount = snapshot.getLong("commentCount") ?: 0L
                transaction.update(
                    postRef,
                    mapOf(
                        "commentCount" to (currentCount - 1L).coerceAtLeast(0L),
                        "updatedAt" to now
                    )
                )
            }.await()
            Resource.Success(Unit)
        }.getOrElse { Resource.Error(friendlyWriteMessage(it, "Unable to delete comment."), it) }
    }

    private suspend fun createCommentNotification(
        ownerId: String,
        actorId: String,
        actorName: String,
        postId: String,
        postTitle: String,
        createdAt: Long
    ) {
        runCatching {
            val ownerSnapshot = firestore.collection("users").document(ownerId).get().await()
            if (ownerSnapshot.getBoolean("commentNotificationsEnabled") != false) {
                val notificationRef = firestore.collection("users")
                    .document(ownerId)
                    .collection("notifications")
                    .document()
                notificationRef.set(
                    mapOf(
                        "id" to notificationRef.id,
                        "title" to "New comment",
                        "body" to "$actorName commented on ${postTitle.ifBlank { "your post" }}",
                        "message" to "$actorName commented on ${postTitle.ifBlank { "your post" }}",
                        "postId" to postId,
                        "type" to "comment",
                        "actorId" to actorId,
                        "senderId" to actorId,
                        "recipientId" to ownerId,
                        "createdAt" to createdAt,
                        "read" to false
                    )
                ).await()
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
}
