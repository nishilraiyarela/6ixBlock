package com.sixblock.app.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sixblock.app.core.model.Resource
import com.sixblock.app.core.model.UiState
import com.sixblock.app.core.util.TorontoDefaults
import com.sixblock.app.domain.model.CommunityPost
import com.sixblock.app.domain.model.PostComment
import com.sixblock.app.domain.model.ReportTargetType
import com.sixblock.app.domain.repository.AuthRepository
import com.sixblock.app.domain.repository.CommentRepository
import com.sixblock.app.domain.repository.LocationRepository
import com.sixblock.app.domain.repository.PostRepository
import com.sixblock.app.domain.repository.ReportRepository
import com.sixblock.app.domain.usecase.AddCommentUseCase
import com.sixblock.app.domain.usecase.ReportContentUseCase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PostDetailViewModel(
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val authRepository: AuthRepository,
    private val locationRepository: LocationRepository,
    private val reportRepository: ReportRepository,
    private val addCommentUseCase: AddCommentUseCase,
    private val reportContentUseCase: ReportContentUseCase
) : ViewModel() {
    private val _postState = MutableLiveData(UiState<CommunityPost>(isLoading = true))
    val postState: LiveData<UiState<CommunityPost>> = _postState

    private val _commentsState = MutableLiveData(UiState<List<PostComment>>(isLoading = true))
    val commentsState: LiveData<UiState<List<PostComment>>> = _commentsState

    private val _actionMessage = MutableLiveData<String?>()
    val actionMessage: LiveData<String?> = _actionMessage
    private val _currentUserId = MutableLiveData<String?>()
    val currentUserId: LiveData<String?> = _currentUserId
    private val _postDeleted = MutableLiveData(false)
    val postDeleted: LiveData<Boolean> = _postDeleted

    fun load(postId: String) {
        viewModelScope.launch {
            authRepository.currentUser.collectLatest { user ->
                _currentUserId.value = user?.id
            }
        }
        viewModelScope.launch {
            refreshPost(postId)
        }
        viewModelScope.launch {
            commentRepository.observeComments(postId).collectLatest { resource ->
                _commentsState.value = when (resource) {
                    Resource.Loading -> UiState(isLoading = true)
                    is Resource.Success -> UiState(data = resource.data, fromCache = resource.fromCache)
                    is Resource.Empty -> UiState(emptyMessage = resource.message)
                    is Resource.Error -> UiState(errorMessage = resource.message)
                }
            }
        }
    }

    private suspend fun refreshPost(postId: String) {
        val origin = when (val location = locationRepository.getBestLocation()) {
            is Resource.Success -> location.data
            else -> TorontoDefaults.center
        }
        _postState.value = when (val post = postRepository.getPost(postId, origin)) {
            is Resource.Success -> UiState(data = post.data, fromCache = post.fromCache)
            is Resource.Error -> UiState(errorMessage = post.message)
            Resource.Loading -> UiState(isLoading = true)
            is Resource.Empty -> UiState(emptyMessage = post.message)
        }
    }

    fun addComment(postId: String, body: String) {
        viewModelScope.launch {
            when (val result = addCommentUseCase(postId, body)) {
                is Resource.Success -> Unit
                is Resource.Error -> _actionMessage.value = result.message
                else -> Unit
            }
        }
    }

    fun editComment(postId: String, commentId: String, body: String) {
        viewModelScope.launch {
            when (val result = commentRepository.editComment(postId, commentId, body)) {
                is Resource.Success -> _actionMessage.value = "Comment updated"
                is Resource.Error -> _actionMessage.value = result.message
                else -> Unit
            }
        }
    }

    fun deleteComment(postId: String, commentId: String) {
        viewModelScope.launch {
            when (val result = commentRepository.deleteComment(postId, commentId)) {
                is Resource.Success -> _actionMessage.value = "Comment deleted"
                is Resource.Error -> _actionMessage.value = result.message
                else -> Unit
            }
        }
    }

    fun editPost(postId: String, title: String, body: String) {
        viewModelScope.launch {
            when (val result = postRepository.editPost(postId, title, body)) {
                is Resource.Success -> {
                    _actionMessage.value = "Post updated"
                    refreshPost(postId)
                }
                is Resource.Error -> _actionMessage.value = result.message
                else -> Unit
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            when (val result = postRepository.deletePost(postId)) {
                is Resource.Success -> {
                    _actionMessage.value = "Post deleted"
                    _postDeleted.value = true
                }
                is Resource.Error -> _actionMessage.value = result.message
                else -> Unit
            }
        }
    }

    fun reportPost(postId: String) {
        viewModelScope.launch {
            when (val result = reportContentUseCase(postId, ReportTargetType.POST, "Reported from post detail")) {
                is Resource.Success -> {
                    reportRepository.hideContent(postId, ReportTargetType.POST)
                    _actionMessage.value = "Post reported and hidden"
                }
                is Resource.Error -> _actionMessage.value = result.message
                else -> Unit
            }
        }
    }

    fun hidePost(postId: String) {
        viewModelScope.launch {
            reportRepository.hideContent(postId, ReportTargetType.POST)
            _actionMessage.value = "Post hidden"
        }
    }
}
