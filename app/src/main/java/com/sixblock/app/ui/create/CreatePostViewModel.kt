package com.sixblock.app.ui.create
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sixblock.app.core.model.Resource
import com.sixblock.app.core.model.UiState
import com.sixblock.app.core.util.GeoUtils
import com.sixblock.app.core.util.TorontoDefaults
import com.sixblock.app.domain.model.GeoPoint
import com.sixblock.app.domain.model.PostCategory
import com.sixblock.app.domain.model.PostDraft
import com.sixblock.app.domain.repository.LocationRepository
import com.sixblock.app.domain.repository.PostRepository
import com.sixblock.app.domain.usecase.CreatePostUseCase
import com.sixblock.app.domain.usecase.SaveDraftUseCase
import kotlinx.coroutines.launch

class CreatePostViewModel(
    private val locationRepository: LocationRepository,
    private val postRepository: PostRepository,
    private val createPostUseCase: CreatePostUseCase,
    private val saveDraftUseCase: SaveDraftUseCase
) : ViewModel() {
    private val _createState = MutableLiveData(UiState<String>())
    val createState: LiveData<UiState<String>> = _createState

    private val _draftState = MutableLiveData<PostDraft?>()
    val draftState: LiveData<PostDraft?> = _draftState

    private val _draftsState = MutableLiveData<List<PostDraft>>()
    val draftsState: LiveData<List<PostDraft>> = _draftsState

    private val _locationLabel = MutableLiveData("Location not shared")
    val locationLabel: LiveData<String> = _locationLabel

    var selectedLocation: GeoPoint = TorontoDefaults.center
        private set
    var approximateArea: String = TorontoDefaults.neighbourhood
        private set
    var selectedAddressLabel: String = ""
        private set
    var shareLocation: Boolean = false
        private set

    init {
        hydrateLocation()
        loadDraft()
    }

    fun setLocation(point: GeoPoint, addressLabel: String? = null) {
        selectedLocation = point
        approximateArea = GeoUtils.coarseArea(point)
        selectedAddressLabel = addressLabel.orEmpty()
        if (shareLocation) _locationLabel.value = locationDisplayLabel()
    }

    fun setShareLocation(enabled: Boolean) {
        shareLocation = enabled
        _locationLabel.value = if (enabled) locationDisplayLabel() else "Location not shared"
        if (enabled) hydrateLocation()
    }

    fun hydrateLocation() {
        viewModelScope.launch {
            val location = locationRepository.getBestLocation()
            if (location is Resource.Success) setLocation(location.data)
        }
    }

    fun loadDraft() {
        viewModelScope.launch {
            _draftState.value = postRepository.getLatestDraft()
            _draftsState.value = postRepository.getDrafts()
        }
    }

    fun refreshDrafts() {
        viewModelScope.launch {
            _draftsState.value = postRepository.getDrafts()
        }
    }

    fun locationDisplayLabel(): String = selectedAddressLabel.ifBlank { approximateArea }

    fun publish(
        title: String,
        body: String,
        category: PostCategory
    ) {
        _createState.value = UiState(isLoading = true)
        viewModelScope.launch {
            _createState.value = when (
                val result = createPostUseCase(
                    title = title,
                    body = body,
                    category = category,
                    location = selectedLocation,
                    approximateArea = if (shareLocation) approximateArea else ""
                )
            ) {
                is Resource.Success -> UiState(data = result.data)
                is Resource.Error -> UiState(errorMessage = result.message)
                Resource.Loading -> UiState(isLoading = true)
                is Resource.Empty -> UiState(emptyMessage = result.message)
            }
        }
    }

    fun updatePost(
        postId: String,
        title: String,
        body: String,
        category: PostCategory
    ) {
        _createState.value = UiState(isLoading = true)
        viewModelScope.launch {
            _createState.value = when (val result = postRepository.editPost(postId, title, body, category)) {
                is Resource.Success -> UiState(data = "post_updated")
                is Resource.Error -> UiState(errorMessage = result.message)
                Resource.Loading -> UiState(isLoading = true)
                is Resource.Empty -> UiState(emptyMessage = result.message)
            }
        }
    }

    fun saveDraft(title: String, body: String, category: PostCategory) {
        viewModelScope.launch {
            saveDraftUseCase(
                title = title,
                body = body,
                category = category,
                localPhotoUri = null,
                location = selectedLocation,
                approximateArea = if (shareLocation) approximateArea else ""
            )
            _draftsState.value = postRepository.getDrafts()
            _createState.value = UiState(data = "draft_saved")
        }
    }
}
