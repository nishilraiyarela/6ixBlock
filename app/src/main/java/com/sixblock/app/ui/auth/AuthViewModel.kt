package com.sixblock.app.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sixblock.app.core.model.Resource
import com.sixblock.app.domain.model.UserProfile
import com.sixblock.app.domain.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _authState = MutableLiveData<Resource<UserProfile>>()
    val authState: LiveData<Resource<UserProfile>> = _authState

    fun signIn(email: String, password: String) {
        _authState.value = Resource.Loading
        viewModelScope.launch {
            _authState.value = authRepository.signIn(email, password)
        }
    }

    fun signUp(
        displayName: String,
        email: String,
        password: String,
        birthday: String = "",
        neighbourhood: String = "",
        gender: String = ""
    ) {
        _authState.value = Resource.Loading
        viewModelScope.launch {
            _authState.value = authRepository.signUp(displayName, email, password, birthday, neighbourhood, gender)
        }
    }

    fun signInWithGoogleToken(idToken: String) {
        _authState.value = Resource.Loading
        viewModelScope.launch {
            _authState.value = authRepository.signInWithGoogle(idToken)
        }
    }
}
