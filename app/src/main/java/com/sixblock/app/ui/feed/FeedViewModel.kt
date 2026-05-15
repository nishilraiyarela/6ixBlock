package com.sixblock.app.ui.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sixblock.app.core.model.Resource
import com.sixblock.app.core.model.UiState
import com.sixblock.app.core.util.GeoUtils
import com.sixblock.app.core.util.TorontoDefaults
import com.sixblock.app.domain.model.CommunityPost
import com.sixblock.app.domain.model.GeoPoint
import com.sixblock.app.domain.model.PostCategory
import com.sixblock.app.domain.model.ReportTargetType
import com.sixblock.app.domain.repository.AuthRepository
import com.sixblock.app.domain.repository.LocationRepository
import com.sixblock.app.domain.repository.PostRepository
import com.sixblock.app.domain.repository.ReportRepository
import com.sixblock.app.domain.usecase.ObserveNearbyPostsUseCase
import com.sixblock.app.domain.usecase.ReportContentUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FeedViewModel(
    private val authRepository: AuthRepository,
    private val locationRepository: LocationRepository,
    private val postRepository: PostRepository,
    private val reportRepository: ReportRepository,
    private val observeNearbyPosts: ObserveNearbyPostsUseCase
) : ViewModel() {
    private val reportContentUseCase = ReportContentUseCase(reportRepository)
    private val _postsState = MutableLiveData(UiState<List<CommunityPost>>(isLoading = true))
    val postsState: LiveData<UiState<List<CommunityPost>>> = _postsState
    private val _currentUserId = MutableLiveData<String?>()
    val currentUserId: LiveData<String?> = _currentUserId
    private val _actionMessage = MutableLiveData<String?>()
    val actionMessage: LiveData<String?> = _actionMessage
    private val _locationTitle = MutableLiveData("${TorontoDefaults.neighbourhood}, ON")
    val locationTitle: LiveData<String> = _locationTitle

    private var selectedCategory: PostCategory? = null
    private var radiusKm = 5
    private var feedJob: Job? = null
    var lastOrigin: GeoPoint = TorontoDefaults.center
        private set

    init {
        viewModelScope.launch {
            authRepository.currentUser.collectLatest { user ->
                _currentUserId.value = user?.id
            }
        }
    }

    fun refresh(category: PostCategory? = selectedCategory, radius: Int = radiusKm, forceFreshLocation: Boolean = false) {
        selectedCategory = category
        radiusKm = radius
        feedJob?.cancel()
        feedJob = viewModelScope.launch {
            _postsState.value = _postsState.value?.copy(isLoading = true, errorMessage = null)
            val origin = when (val location = locationRepository.getBestLocation()) {
                is Resource.Success -> location.data
                else -> TorontoDefaults.center
            }
            lastOrigin = origin
            val area = if (GeoUtils.distanceKm(origin, TorontoDefaults.center) < 0.05) {
                TorontoDefaults.neighbourhood
            } else {
                GeoUtils.coarseArea(origin)
            }
            _locationTitle.value = "$area, ON"
            observeNearbyPosts(origin, radiusKm, selectedCategory).collectLatest { resource ->
                _postsState.value = when (resource) {
                    Resource.Loading -> UiState(isLoading = true, data = _postsState.value?.data)
                    is Resource.Success -> UiState(data = resource.data, fromCache = resource.fromCache)
                    is Resource.Empty -> UiState(emptyMessage = resource.message)
                    is Resource.Error -> UiState(errorMessage = resource.message, data = _postsState.value?.data)
                }
            }
        }
    }

    fun editPost(postId: String, title: String, body: String) {
        viewModelScope.launch {
            when (val result = postRepository.editPost(postId, title, body)) {
                is Resource.Success -> {
                    _actionMessage.value = "Post updated"
                    refresh()
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
                    refresh()
                }
                is Resource.Error -> _actionMessage.value = result.message
                else -> Unit
            }
        }
    }

    fun toggleLike(post: CommunityPost) {
        val currentPosts = _postsState.value?.data.orEmpty()
        _postsState.value = _postsState.value?.copy(
            data = currentPosts.map { item ->
                if (item.id == post.id) {
                    val nextLiked = !post.likedByCurrentUser
                    item.copy(
                        likedByCurrentUser = nextLiked,
                        likeCount = (item.likeCount + if (nextLiked) 1 else -1).coerceAtLeast(0)
                    )
                } else {
                    item
                }
            }
        )
        viewModelScope.launch {
            when (val result = postRepository.togglePostLike(post.id, post.likedByCurrentUser)) {
                is Resource.Success -> Unit
                is Resource.Error -> {
                    refresh()
                }
                else -> Unit
            }
        }
    }

    fun toggleSave(post: CommunityPost) {
        val currentPosts = _postsState.value?.data.orEmpty()
        _postsState.value = _postsState.value?.copy(
            data = currentPosts.map { item ->
                if (item.id == post.id) item.copy(savedByCurrentUser = !post.savedByCurrentUser) else item
            }
        )
        viewModelScope.launch {
            when (val result = postRepository.togglePostSave(post.id, post.savedByCurrentUser)) {
                is Resource.Success -> Unit
                is Resource.Error -> {
                    _actionMessage.value = result.message
                    refresh()
                }
                else -> Unit
            }
        }
    }

    fun hidePost(postId: String) {
        viewModelScope.launch {
            reportRepository.hideContent(postId, ReportTargetType.POST)
            _actionMessage.value = "Post hidden"
            refresh()
        }
    }

    fun reportPost(postId: String, hideAfter: Boolean) {
        viewModelScope.launch {
            when (val result = reportContentUseCase(postId, ReportTargetType.POST, "Reported from feed")) {
                is Resource.Success -> {
                    if (hideAfter) reportRepository.hideContent(postId, ReportTargetType.POST)
                    _actionMessage.value = if (hideAfter) "Post reported and hidden" else "Post reported"
                    refresh()
                }
                is Resource.Error -> _actionMessage.value = result.message
                else -> Unit
            }
        }
    }
}
