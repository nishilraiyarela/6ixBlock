package com.sixblock.app.domain.repository

import com.sixblock.app.core.model.Resource
import com.sixblock.app.domain.model.PostComment
import kotlinx.coroutines.flow.Flow

interface CommentRepository {
    fun observeComments(postId: String): Flow<Resource<List<PostComment>>>
    suspend fun addComment(postId: String, body: String): Resource<String>
    suspend fun editComment(postId: String, commentId: String, body: String): Resource<Unit>
    suspend fun deleteComment(postId: String, commentId: String): Resource<Unit>
}
