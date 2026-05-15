package com.sixblock.app.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sixblock.app.core.model.Resource
import com.sixblock.app.core.model.UiState
import com.sixblock.app.core.util.TorontoDefaults
import com.sixblock.app.domain.model.CommunityPost
import com.sixblock.app.domain.model.PostCategory
import com.sixblock.app.domain.model.UserProfile
import com.sixblock.app.domain.repository.AuthRepository
import com.sixblock.app.domain.repository.PostRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val postRepository: PostRepository
) : ViewModel() {
    private val _profileState = MutableLiveData<UserProfile?>()
    val profileState: LiveData<UserProfile?> = _profileState
    private val _currentUserId = MutableLiveData<String?>()
    val currentUserId: LiveData<String?> = _currentUserId
    private val _myPostsState = MutableLiveData(UiState<List<CommunityPost>>(isLoading = true))
    val myPostsState: LiveData<UiState<List<CommunityPost>>> = _myPostsState
    private val _savedPostsState = MutableLiveData(UiState<List<CommunityPost>>(isLoading = true))
    val savedPostsState: LiveData<UiState<List<CommunityPost>>> = _savedPostsState
    private val _alertsState = MutableLiveData(UiState<List<CommunityPost>>(isLoading = true))
    val alertsState: LiveData<UiState<List<CommunityPost>>> = _alertsState
    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message
    private var loaded = false

    fun load() {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            authRepository.currentUser.collectLatest { profile ->
                _profileState.value = profile
                _currentUserId.value = profile?.id
            }
        }
        viewModelScope.launch {
            authRepository.currentUser.collectLatest { profile ->
                if (profile == null) {
                    _myPostsState.value = UiState(data = emptyList(), emptyMessage = "Sign in to see your posts.")
                    _alertsState.value = UiState(data = emptyList(), emptyMessage = "Sign in to see your alerts.")
                    return@collectLatest
                }
                postRepository.observeUserPosts(profile.id, TorontoDefaults.center).collectLatest { resource ->
                    _myPostsState.value = when (resource) {
                        Resource.Loading -> UiState(isLoading = true, data = _myPostsState.value?.data)
                        is Resource.Success -> {
                            val posts = resource.data
                            UiState(data = posts, emptyMessage = if (posts.isEmpty()) "You haven't posted anything yet." else null)
                        }
                        is Resource.Empty -> UiState(data = emptyList(), emptyMessage = "You haven't posted anything yet.")
                        is Resource.Error -> UiState(errorMessage = resource.message, data = _myPostsState.value?.data)
                    }
                    val posts = _myPostsState.value?.data.orEmpty()
                    val alerts = posts.filter { it.category == PostCategory.SAFETY_ALERT }
                    _alertsState.value = UiState(data = alerts, emptyMessage = if (alerts.isEmpty()) "Your alerts will appear here." else null)
                }
            }
        }
        viewModelScope.launch {
            postRepository.observeSavedPosts(TorontoDefaults.center).collectLatest { resource ->
                _savedPostsState.value = when (resource) {
                    Resource.Loading -> UiState(isLoading = true, data = _savedPostsState.value?.data)
                    is Resource.Success -> UiState(data = resource.data, fromCache = resource.fromCache)
                    is Resource.Empty -> UiState(data = emptyList(), emptyMessage = resource.message)
                    is Resource.Error -> UiState(errorMessage = resource.message, data = _savedPostsState.value?.data)
                }
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun updateProfileInfo(email: String, photoUrl: String, neighbourhood: String, gender: String) {
        viewModelScope.launch {
            when (val result = authRepository.updateProfileInfo(email, photoUrl, neighbourhood, gender)) {
                is Resource.Success -> {
                    _profileState.value = result.data
                    _message.value = "Profile info saved"
                }
                is Resource.Error -> _message.value = result.message
                is Resource.Empty -> _message.value = result.message
                Resource.Loading -> Unit
            }
        }
    }

    fun toggleLike(post: CommunityPost) {
        viewModelScope.launch {
            when (val result = postRepository.togglePostLike(post.id, post.likedByCurrentUser)) {
                is Resource.Error -> Unit
                is Resource.Empty -> _message.value = result.message
                else -> Unit
            }
        }
    }

    fun toggleSave(post: CommunityPost) {
        viewModelScope.launch {
            when (val result = postRepository.togglePostSave(post.id, post.savedByCurrentUser)) {
                is Resource.Success -> Unit
                is Resource.Error -> _message.value = result.message
                is Resource.Empty -> _message.value = result.message
                Resource.Loading -> Unit
            }
        }
    }

}
