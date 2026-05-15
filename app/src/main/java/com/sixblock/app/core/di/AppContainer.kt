package com.sixblock.app.core.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.sixblock.app.data.local.SixBlockDatabase
import com.sixblock.app.data.remote.firebase.FirebaseAuthRepository
import com.sixblock.app.data.remote.firebase.FirebaseCommentRepository
import com.sixblock.app.data.remote.firebase.FirebaseNotificationRepository
import com.sixblock.app.data.remote.firebase.FirebasePostRepository
import com.sixblock.app.data.remote.firebase.FirebaseReportRepository
import com.sixblock.app.data.remote.location.AndroidLocationRepository
import com.sixblock.app.domain.repository.AuthRepository
import com.sixblock.app.domain.repository.CommentRepository
import com.sixblock.app.domain.repository.LocationRepository
import com.sixblock.app.domain.repository.NotificationRepository
import com.sixblock.app.domain.repository.PostRepository
import com.sixblock.app.domain.repository.ReportRepository

open class AppContainer protected constructor(context: Context) {
    protected val appContext: Context = context.applicationContext

    val database: SixBlockDatabase = Room.databaseBuilder(
        appContext,
        SixBlockDatabase::class.java,
        "sixblock.db"
    ).build()

    open val authRepository: AuthRepository by lazy { error("AuthRepository is not configured.") }
    open val locationRepository: LocationRepository by lazy { AndroidLocationRepository(appContext) }
    open val postRepository: PostRepository by lazy { error("PostRepository is not configured.") }
    open val commentRepository: CommentRepository by lazy { error("CommentRepository is not configured.") }
    open val reportRepository: ReportRepository by lazy { error("ReportRepository is not configured.") }
    open val notificationRepository: NotificationRepository by lazy { error("NotificationRepository is not configured.") }

    private class FirebaseAppContainer(context: Context) : AppContainer(context) {
        private val auth by lazy { FirebaseAuth.getInstance() }
        private val firestore by lazy { FirebaseFirestore.getInstance() }
        private val messaging by lazy { FirebaseMessaging.getInstance() }

        override val authRepository: AuthRepository by lazy {
            FirebaseAuthRepository(auth, firestore, messaging)
        }
        override val postRepository: PostRepository by lazy {
            FirebasePostRepository(
                firestore = firestore,
                auth = auth,
                postDao = database.postDao(),
                draftDao = database.draftDao(),
                hiddenContentDao = database.hiddenContentDao()
            )
        }
        override val commentRepository: CommentRepository by lazy {
            FirebaseCommentRepository(
                firestore = firestore,
                auth = auth,
                commentDao = database.commentDao()
            )
        }
        override val reportRepository: ReportRepository by lazy {
            FirebaseReportRepository(
                firestore = firestore,
                auth = auth,
                hiddenContentDao = database.hiddenContentDao()
            )
        }
        override val notificationRepository: NotificationRepository by lazy {
            FirebaseNotificationRepository(firestore, auth)
        }
    }

    companion object {
        fun createFirebase(context: Context): AppContainer = FirebaseAppContainer(context)
    }
}
