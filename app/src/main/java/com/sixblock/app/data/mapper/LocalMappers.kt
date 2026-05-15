package com.sixblock.app.data.mapper

import com.sixblock.app.core.util.GeoUtils
import com.sixblock.app.domain.model.CommentStatus
import com.sixblock.app.domain.model.CommunityPost
import com.sixblock.app.domain.model.GeoPoint
import com.sixblock.app.domain.model.PostCategory
import com.sixblock.app.domain.model.PostComment
import com.sixblock.app.domain.model.PostDraft
import com.sixblock.app.domain.model.PostStatus
import com.sixblock.app.data.local.entity.CachedCommentEntity
import com.sixblock.app.data.local.entity.CachedPostEntity
import com.sixblock.app.data.local.entity.PostDraftEntity

fun CachedPostEntity.toDomain(origin: GeoPoint? = null): CommunityPost {
    val point = GeoPoint(latitude, longitude)
    return CommunityPost(
        id = id,
        title = title,
        body = body,
        category = PostCategory.fromId(categoryId),
        authorId = authorId,
        authorName = authorName,
        location = point,
        geohash = geohash,
        cityKey = cityKey,
        approximateArea = GeoUtils.publicAreaLabel(approximateArea, point),
        imageUrl = imageUrl,
        createdAt = createdAt,
        updatedAt = updatedAt,
        commentCount = commentCount,
        reportCount = reportCount,
        status = PostStatus.fromId(statusId),
        distanceKm = origin?.let { GeoUtils.distanceKm(it, point) }
    )
}

fun CommunityPost.toEntity(): CachedPostEntity = CachedPostEntity(
    id = id,
    title = title,
    body = body,
    categoryId = category.id,
    authorId = authorId,
    authorName = authorName,
    latitude = location.latitude,
    longitude = location.longitude,
    geohash = geohash,
    cityKey = cityKey,
    approximateArea = approximateArea,
    imageUrl = imageUrl,
    createdAt = createdAt,
    updatedAt = updatedAt,
    commentCount = commentCount,
    reportCount = reportCount,
    statusId = status.id
)

fun CachedCommentEntity.toDomain(): PostComment = PostComment(
    id = id,
    postId = postId,
    body = body,
    authorId = authorId,
    authorName = authorName,
    createdAt = createdAt,
    updatedAt = createdAt,
    edited = false,
    reportCount = reportCount,
    status = CommentStatus.fromId(statusId)
)

fun PostComment.toEntity(): CachedCommentEntity = CachedCommentEntity(
    id = id,
    postId = postId,
    body = body,
    authorId = authorId,
    authorName = authorName,
    createdAt = createdAt,
    reportCount = reportCount,
    statusId = status.id
)

fun PostDraftEntity.toDomain(): PostDraft = PostDraft(
    id = id,
    title = title,
    body = body,
    category = PostCategory.fromId(categoryId),
    localPhotoUri = localPhotoUri,
    location = GeoPoint(latitude, longitude),
    approximateArea = approximateArea,
    updatedAt = updatedAt
)

fun PostDraft.toEntity(): PostDraftEntity = PostDraftEntity(
    id = id,
    title = title,
    body = body,
    categoryId = category.id,
    localPhotoUri = localPhotoUri,
    latitude = location.latitude,
    longitude = location.longitude,
    approximateArea = approximateArea,
    updatedAt = updatedAt
)
