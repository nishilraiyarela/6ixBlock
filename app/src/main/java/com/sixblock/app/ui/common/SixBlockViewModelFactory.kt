package com.sixblock.app.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sixblock.app.core.di.AppContainer
import com.sixblock.app.domain.usecase.AddCommentUseCase
import com.sixblock.app.domain.usecase.CreatePostUseCase
import com.sixblock.app.domain.usecase.ObserveNearbyPostsUseCase
import com.sixblock.app.domain.usecase.ReportContentUseCase
import com.sixblock.app.domain.usecase.SaveDraftUseCase
import com.sixblock.app.ui.activity.ActivityViewModel
import com.sixblock.app.ui.auth.AuthViewModel
import com.sixblock.app.ui.create.CreatePostViewModel
import com.sixblock.app.ui.detail.PostDetailViewModel
import com.sixblock.app.ui.feed.FeedViewModel
import com.sixblock.app.ui.profile.ProfileViewModel
import com.sixblock.app.ui.saved.SavedPostsViewModel

class SixBlockViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> AuthViewModel(container.authRepository) as T
            modelClass.isAssignableFrom(FeedViewModel::class.java) -> FeedViewModel(
                authRepository = container.authRepository,
                locationRepository = container.locationRepository,
                postRepository = container.postRepository,
                reportRepository = container.reportRepository,
                observeNearbyPosts = ObserveNearbyPostsUseCase(container.postRepository)
            ) as T
            modelClass.isAssignableFrom(CreatePostViewModel::class.java) -> CreatePostViewModel(
                locationRepository = container.locationRepository,
                postRepository = container.postRepository,
                createPostUseCase = CreatePostUseCase(container.postRepository),
                saveDraftUseCase = SaveDraftUseCase(container.postRepository)
            ) as T
            modelClass.isAssignableFrom(PostDetailViewModel::class.java) -> PostDetailViewModel(
                postRepository = container.postRepository,
                commentRepository = container.commentRepository,
                authRepository = container.authRepository,
                locationRepository = container.locationRepository,
                reportRepository = container.reportRepository,
                addCommentUseCase = AddCommentUseCase(container.commentRepository),
                reportContentUseCase = ReportContentUseCase(container.reportRepository)
            ) as T
            modelClass.isAssignableFrom(ActivityViewModel::class.java) -> ActivityViewModel(container.notificationRepository) as T
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> ProfileViewModel(
                authRepository = container.authRepository,
                postRepository = container.postRepository
            ) as T
            modelClass.isAssignableFrom(SavedPostsViewModel::class.java) -> SavedPostsViewModel(
                authRepository = container.authRepository,
                locationRepository = container.locationRepository,
                postRepository = container.postRepository
            ) as T
            else -> error("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
