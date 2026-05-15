package com.sixblock.app.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.sixblock.app.core.model.Resource
import com.sixblock.app.domain.model.NotificationItem
import com.sixblock.app.domain.repository.NotificationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseNotificationRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : NotificationRepository {

    override fun observeNotifications(): Flow<Resource<List<NotificationItem>>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(Resource.Empty("Sign in to see activity."))
            close()
            return@callbackFlow
        }
        trySend(Resource.Loading)
        val registration = firestore.collection("users")
            .document(userId)
            .collection("notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Unable to load activity.", error))
                    return@addSnapshotListener
                }
                val notifications = snapshot?.documents.orEmpty().map { it.toNotificationItem() }
                if (notifications.isEmpty()) trySend(Resource.Empty("No activity yet."))
                else trySend(Resource.Success(notifications))
            }

        awaitClose { registration.remove() }
    }

    override suspend fun markRead(notificationId: String) {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users")
            .document(userId)
            .collection("notifications")
            .document(notificationId)
            .update("read", true)
            .await()
    }

    override suspend fun markAllRead() {
        val userId = auth.currentUser?.uid ?: return
        val unread = firestore.collection("users")
            .document(userId)
            .collection("notifications")
            .whereEqualTo("read", false)
            .get()
            .await()
        if (unread.isEmpty) return
        val batch = firestore.batch()
        unread.documents.forEach { document ->
            batch.update(document.reference, "read", true)
        }
        batch.commit().await()
    }

    override suspend fun setNotificationPreference(key: String, enabled: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users")
            .document(userId)
            .set(mapOf(key to enabled), SetOptions.merge())
            .await()
    }
}
