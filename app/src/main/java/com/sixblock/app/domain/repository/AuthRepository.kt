package com.sixblock.app.domain.repository

import com.sixblock.app.core.model.Resource
import com.sixblock.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<UserProfile?>
    suspend fun signIn(email: String, password: String): Resource<UserProfile>
    suspend fun signUp(
        displayName: String,
        email: String,
        password: String,
        birthday: String = "",
        neighbourhood: String = "",
        gender: String = ""
    ): Resource<UserProfile>
    suspend fun signInWithGoogle(idToken: String): Resource<UserProfile>
    suspend fun updateProfileInfo(email: String, photoUrl: String, neighbourhood: String, gender: String): Resource<UserProfile>
    suspend fun registerFcmToken()
    fun signOut()
}
