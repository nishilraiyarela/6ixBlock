package com.sixblock.app.ui.activity

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sixblock.app.core.model.Resource
import com.sixblock.app.core.model.UiState
import com.sixblock.app.domain.model.NotificationItem
import com.sixblock.app.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ActivityViewModel(private val notificationRepository: NotificationRepository) : ViewModel() {
    private val _activityState = MutableLiveData(UiState<List<NotificationItem>>(isLoading = true))
    val activityState: LiveData<UiState<List<NotificationItem>>> = _activityState

    fun load() {
        viewModelScope.launch {
            notificationRepository.observeNotifications().collectLatest { resource ->
                _activityState.value = when (resource) {
                    Resource.Loading -> UiState(isLoading = true)
                    is Resource.Success -> UiState(data = resource.data)
                    is Resource.Empty -> UiState(emptyMessage = resource.message)
                    is Resource.Error -> UiState(errorMessage = resource.message)
                }
            }
        }
    }

    fun markRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markRead(notificationId)
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            notificationRepository.markAllRead()
        }
    }
}
