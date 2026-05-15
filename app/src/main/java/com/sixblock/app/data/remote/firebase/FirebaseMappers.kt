package com.sixblock.app.data.remote.firebase

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.sixblock.app.core.util.GeoUtils
import com.sixblock.app.core.util.TorontoDefaults
import com.sixblock.app.domain.model.CommentStatus
import com.sixblock.app.domain.model.CommunityPost
import com.sixblock.app.domain.model.GeoPoint
import com.sixblock.app.domain.model.NotificationItem
import com.sixblock.app.domain.model.NotificationType
import com.sixblock.app.domain.model.PostCategory
import com.sixblock.app.domain.model.PostComment
import com.sixblock.app.domain.model.PostStatus
import com.sixblock.app.domain.model.UserProfile

fun FirebaseUser.toProfile(fallbackName: String? = null): UserProfile = UserProfile(
    id = uid,
    displayName = fallbackName?.takeIf { it.isNotBlank() }
        ?: displayName?.takeIf { it.isNotBlank() }
        ?: email?.substringBefore("@")
        ?: "Neighbour",
    email = email,
    photoUrl = photoUrl?.toString()
)

fun DocumentSnapshot.postOwnerId(): String =
    getString("authorId")
        ?: getString("userId")
        ?: getString("createdBy")
        ?: getString("uid")
        ?: ""

fun DocumentSnapshot.toCommunityPost(origin: GeoPoint? = null): CommunityPost? {
    val latitude = getDouble("latitude") ?: return null
    val longitude = getDouble("longitude") ?: return null
    val point = GeoPoint(latitude, longitude)
    return CommunityPost(
        id = getString("id") ?: id,
        title = getString("title").orEmpty(),
        body = getString("body").orEmpty(),
        category = PostCategory.fromId(getString("categoryId")),
        authorId = postOwnerId(),
        authorName = getString("authorName") ?: "Neighbour",
        location = point,
        geohash = getString("geohash").orEmpty(),
        cityKey = getString("cityKey") ?: TorontoDefaults.cityKey,
        approximateArea = GeoUtils.publicAreaLabel(getString("approximateArea"), point),
        imageUrl = getString("imageUrl"),
        createdAt = getLong("createdAt") ?: 0L,
        updatedAt = getLong("updatedAt") ?: 0L,
        commentCount = (getLong("commentCount") ?: 0L).toInt().coerceAtLeast(0),
        likeCount = (getLong("likeCount") ?: 0L).toInt().coerceAtLeast(0),
        reportCount = (getLong("reportCount") ?: 0L).toInt().coerceAtLeast(0),
        status = PostStatus.fromId(getString("status")),
        distanceKm = origin?.let { GeoUtils.distanceKm(it, point) }
    )
}

fun DocumentSnapshot.toPostComment(postId: String): PostComment = PostComment(
    id = getString("id") ?: id,
    postId = getString("postId") ?: postId,
    body = getString("body").orEmpty(),
    authorId = getString("authorId").orEmpty(),
    authorName = getString("authorName") ?: "Neighbour",
    createdAt = getLong("createdAt") ?: 0L,
    updatedAt = getLong("updatedAt") ?: getLong("createdAt") ?: 0L,
    edited = getBoolean("edited") ?: false,
    reportCount = (getLong("reportCount") ?: 0L).toInt(),
    status = CommentStatus.fromId(getString("status"))
)

fun DocumentSnapshot.toNotificationItem(): NotificationItem = NotificationItem(
    id = getString("id") ?: id,
    title = getString("title") ?: "6ixBlock",
    body = getString("body") ?: getString("message").orEmpty(),
    postId = getString("postId"),
    type = NotificationType.fromId(getString("type")),
    createdAt = getLong("createdAt") ?: 0L,
    read = getBoolean("read") ?: false
)
