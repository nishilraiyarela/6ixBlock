package com.sixblock.app.ui.saved

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sixblock.app.core.model.Resource
import com.sixblock.app.core.model.UiState
import com.sixblock.app.core.util.TorontoDefaults
import com.sixblock.app.domain.model.CommunityPost
import com.sixblock.app.domain.model.GeoPoint
import com.sixblock.app.domain.repository.AuthRepository
import com.sixblock.app.domain.repository.LocationRepository
import com.sixblock.app.domain.repository.PostRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SavedPostsViewModel(
    private val authRepository: AuthRepository,
    private val locationRepository: LocationRepository,
    private val postRepository: PostRepository
) : ViewModel() {
    private val _postsState = MutableLiveData(UiState<List<CommunityPost>>(isLoading = true))
    val postsState: LiveData<UiState<List<CommunityPost>>> = _postsState

    private val _currentUserId = MutableLiveData<String?>()
    val currentUserId: LiveData<String?> = _currentUserId

    private val _actionMessage = MutableLiveData<String?>()
    val actionMessage: LiveData<String?> = _actionMessage

    init {
        viewModelScope.launch {
            authRepository.currentUser.collectLatest { user ->
                _currentUserId.value = user?.id
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            val origin: GeoPoint = when (val location = locationRepository.getBestLocation()) {
                is Resource.Success -> location.data
                else -> TorontoDefaults.center
            }
            postRepository.observeSavedPosts(origin).collectLatest { resource ->
                _postsState.value = when (resource) {
                    Resource.Loading -> UiState(isLoading = true, data = _postsState.value?.data)
                    is Resource.Success -> UiState(data = resource.data, fromCache = resource.fromCache)
                    is Resource.Empty -> UiState(emptyMessage = resource.message)
                    is Resource.Error -> UiState(errorMessage = resource.message, data = _postsState.value?.data)
                }
            }
        }
    }

    fun toggleLike(post: CommunityPost) {
        viewModelScope.launch {
            when (val result = postRepository.togglePostLike(post.id, post.likedByCurrentUser)) {
                is Resource.Error -> Unit
                else -> Unit
            }
        }
    }

    fun toggleSave(post: CommunityPost) {
        viewModelScope.launch {
            when (val result = postRepository.togglePostSave(post.id, post.savedByCurrentUser)) {
                is Resource.Success -> Unit
                is Resource.Error -> _actionMessage.value = result.message
                else -> Unit
            }
        }
    }
}
