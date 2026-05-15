package com.sixblock.app.data.remote.firebase

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.sixblock.app.core.model.Resource
import com.sixblock.app.core.util.TorontoDefaults
import com.sixblock.app.domain.model.UserProfile
import com.sixblock.app.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val messaging: FirebaseMessaging
) : AuthRepository {

    override val currentUser: Flow<UserProfile?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user == null) {
                trySend(null)
            } else {
                launch {
                    val baseProfile = user.toProfile()
                    val document = runCatching {
                        firestore.collection("users").document(user.uid).get().await()
                    }.getOrNull()
                    trySend(
                        baseProfile.copy(
                            photoUrl = document?.getString("photoUrl") ?: baseProfile.photoUrl,
                            email = document?.getString("email") ?: baseProfile.email,
                            birthday = document?.getString("birthday").orEmpty(),
                            neighbourhood = document?.getString("neighbourhood").orEmpty(),
                            gender = document?.getString("gender").orEmpty(),
                            radiusKm = (document?.getLong("radiusKm") ?: baseProfile.radiusKm.toLong()).toInt(),
                            createdAt = document?.getLong("createdAt") ?: baseProfile.createdAt
                        )
                    )
                }
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signIn(email: String, password: String): Resource<UserProfile> {
        return runCatching {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val profile = result.user?.toProfile() ?: error("No Firebase user returned.")
            upsertProfile(profile)
            registerFcmToken()
            Resource.Success(profile)
        }.getOrElse { Resource.Error(friendlyAuthMessage(it, "Unable to sign in. Please try again."), it) }
    }

    override suspend fun signUp(
        displayName: String,
        email: String,
        password: String,
        birthday: String,
        neighbourhood: String,
        gender: String
    ): Resource<UserProfile> {
        return runCatching {
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = result.user ?: error("No Firebase user returned.")
            user.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName.trim())
                    .build()
            ).await()
            val profile = user.toProfile(displayName.trim()).copy(
                birthday = birthday.trim(),
                neighbourhood = neighbourhood.trim(),
                gender = gender.trim()
            )
            upsertProfile(profile, includeCreatedAt = true)
            registerFcmToken()
            Resource.Success(profile)
        }.getOrElse { Resource.Error(friendlyAuthMessage(it, "Unable to create account. Please try again."), it) }
    }

    override suspend fun signInWithGoogle(idToken: String): Resource<UserProfile> {
        return runCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val profile = result.user?.toProfile() ?: error("No Firebase user returned.")
            upsertProfile(profile)
            registerFcmToken()
            Resource.Success(profile)
        }.getOrElse { Resource.Error(friendlyAuthMessage(it, "Unable to continue with Google. Please try again."), it) }
    }

    override suspend fun updateProfileInfo(email: String, photoUrl: String, neighbourhood: String, gender: String): Resource<UserProfile> {
        return runCatching {
            val user = auth.currentUser ?: error("No signed-in user.")
            firestore.collection("users")
                .document(user.uid)
                .set(
                    mapOf(
                        "email" to email.trim().ifBlank { user.email.orEmpty() },
                        "photoUrl" to photoUrl.trim(),
                        "neighbourhood" to neighbourhood.trim(),
                        "gender" to gender.trim(),
                        "updatedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )
                .await()
            user.toProfile().copy(
                email = email.trim().ifBlank { user.email },
                photoUrl = photoUrl.trim().ifBlank { user.photoUrl?.toString() },
                neighbourhood = neighbourhood.trim(),
                gender = gender.trim()
            )
        }.fold(
            onSuccess = { Resource.Success(it) },
            onFailure = { Resource.Error(friendlyAuthMessage(it, "Unable to update profile. Please try again."), it) }
        )
    }

    override suspend fun registerFcmToken() {
        val userId = auth.currentUser?.uid ?: return
        val token = messaging.token.await()
        firestore.collection("users")
            .document(userId)
            .collection("deviceTokens")
            .document(token)
            .set(
                mapOf(
                    "token" to token,
                    "platform" to "android",
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            .await()
    }

    override fun signOut() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            auth.signOut()
            return
        }
        messaging.token
            .addOnSuccessListener { token ->
                firestore.collection("users")
                    .document(userId)
                    .collection("deviceTokens")
                    .document(token)
                    .delete()
                    .addOnCompleteListener { auth.signOut() }
            }
            .addOnFailureListener { auth.signOut() }
    }

    private suspend fun upsertProfile(profile: UserProfile, includeCreatedAt: Boolean = false) {
        val payload = mutableMapOf<String, Any?>(
            "id" to profile.id,
            "displayName" to profile.displayName,
            "email" to profile.email,
            "homeCityKey" to TorontoDefaults.cityKey,
            "radiusKm" to profile.radiusKm,
            "updatedAt" to System.currentTimeMillis()
        )
        if (includeCreatedAt) payload["createdAt"] = profile.createdAt
        if (!profile.photoUrl.isNullOrBlank()) payload["photoUrl"] = profile.photoUrl
        if (profile.birthday.isNotBlank()) payload["birthday"] = profile.birthday
        if (profile.neighbourhood.isNotBlank()) payload["neighbourhood"] = profile.neighbourhood
        if (profile.gender.isNotBlank()) payload["gender"] = profile.gender
        firestore.collection("users")
            .document(profile.id)
            .set(payload, SetOptions.merge())
            .await()
    }

    private fun friendlyAuthMessage(error: Throwable, fallback: String): String {
        return when (error) {
            is FirebaseAuthInvalidCredentialsException -> {
                val message = error.message.orEmpty().lowercase()
                if ("password" in message) "Your password is incorrect. Please try again."
                else "Please enter a valid email address."
            }
            is FirebaseAuthInvalidUserException -> "No account found with this email."
            is FirebaseAuthUserCollisionException -> "This email is already registered. Try logging in instead."
            is FirebaseNetworkException -> "Please check your internet connection and try again."
            else -> fallback
        }
    }
}
