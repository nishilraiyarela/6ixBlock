package com.sixblock.app.domain.usecase

import com.sixblock.app.core.model.Resource
import com.sixblock.app.domain.repository.CommentRepository

class AddCommentUseCase(private val commentRepository: CommentRepository) {
    suspend operator fun invoke(postId: String, body: String): Resource<String> {
        val cleanBody = body.trim()
        if (cleanBody.length < 2) return Resource.Error("Comment is too short.")
        return commentRepository.addComment(postId, cleanBody)
    }
}
