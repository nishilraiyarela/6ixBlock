package com.sixblock.app.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sixblock.app.SixBlockApplication
import com.sixblock.app.R
import com.sixblock.app.core.model.Resource
import com.sixblock.app.core.util.AppSettings
import com.sixblock.app.core.util.TorontoDefaults
import com.sixblock.app.data.remote.messaging.SixBlockNotificationPresenter
import com.sixblock.app.databinding.ActivityMainBinding
import com.sixblock.app.domain.model.CommunityPost
import com.sixblock.app.domain.model.NotificationItem
import com.sixblock.app.ui.activity.ActivityFragment
import com.sixblock.app.ui.create.CreatePostFragment
import com.sixblock.app.ui.feed.FeedFragment
import com.sixblock.app.ui.map.MapFragment
import com.sixblock.app.ui.profile.ProfileFragment
import com.sixblock.app.ui.saved.SavedPostsFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        AppSettings.markNotificationPermissionAsked(this)
    }
    private val tags = mapOf(
        R.id.nav_feed to "feed",
        R.id.nav_map to "map",
        R.id.nav_create to "create",
        R.id.nav_activity to "activity",
        R.id.nav_profile to "profile"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemInsets()

        if (savedInstanceState == null) {
            addRootFragments()
            showRoot(R.id.nav_feed, immediate = true)
            handleLaunchIntent()
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            supportFragmentManager.popBackStackImmediate()
            showRoot(item.itemId)
            onRootTabOpened(item.itemId)
            true
        }
        setupBackConfirmation()
        askNotificationPermissionIfNeeded()
        registerFcmTokenIfSignedIn()
        observeUnreadActivityBadge()
        observeFeedUpdateBadge()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent()
    }

    private fun applySystemInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, 0)
            binding.bottomNav.setPadding(0, 0, 0, bars.bottom)
            insets
        }
    }

    private fun addRootFragments() {
        val roots = mapOf(
            R.id.nav_feed to FeedFragment(),
            R.id.nav_map to MapFragment(),
            R.id.nav_create to CreatePostFragment(),
            R.id.nav_activity to ActivityFragment(),
            R.id.nav_profile to ProfileFragment()
        )
        supportFragmentManager.beginTransaction().apply {
            roots.forEach { (id, fragment) ->
                add(R.id.mainContainer, fragment, tags.getValue(id))
                hide(fragment)
            }
        }.commitNow()
    }

    private fun showRoot(itemId: Int, immediate: Boolean = false) {
        val tag = tags.getValue(itemId)
        val target = supportFragmentManager.findFragmentByTag(tag) ?: return
        val transaction = supportFragmentManager.beginTransaction().apply {
            tags.values.mapNotNull { supportFragmentManager.findFragmentByTag(it) }.forEach(::hide)
            show(target)
        }
        if (immediate) transaction.commitNow() else transaction.commit()
    }

    fun openFeed() {
        openRootTab(R.id.nav_feed)
    }

    fun openActivityTab() {
        openRootTab(R.id.nav_activity)
    }

    fun openMapTab() {
        openRootTab(R.id.nav_map)
    }

    fun openCreateTab() {
        openRootTab(R.id.nav_create)
    }

    fun openProfileTab() {
        openRootTab(R.id.nav_profile)
    }

    private fun openRootTab(itemId: Int) {
        supportFragmentManager.popBackStackImmediate()
        binding.bottomNav.menu.findItem(itemId).isChecked = true
        showRoot(itemId, immediate = true)
        onRootTabOpened(itemId)
    }

    fun openCreateForEdit(post: CommunityPost) {
        supportFragmentManager.popBackStackImmediate()
        binding.bottomNav.menu.findItem(R.id.nav_create).isChecked = true
        showRoot(R.id.nav_create, immediate = true)
        (supportFragmentManager.findFragmentByTag(tags.getValue(R.id.nav_create)) as? CreatePostFragment)
            ?.startEditing(post)
    }

    fun openDetail(postId: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.mainContainer, com.sixblock.app.ui.detail.PostDetailFragment.newInstance(postId))
            .addToBackStack("post-detail")
            .commit()
    }

    fun openFullScreen(fragment: Fragment, tag: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.mainContainer, fragment, tag)
            .addToBackStack(tag)
            .commit()
    }

    fun openSavedPosts() {
        openFullScreen(SavedPostsFragment(), "saved-posts")
    }

    private fun setupBackConfirmation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else {
                    showExitDialog()
                }
            }
        })
    }

    private fun showExitDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.exit_dialog_title)
            .setNegativeButton(R.string.go_back, null)
            .setPositiveButton(R.string.exit) { _, _ -> finish() }
            .show()
    }

    private fun handleLaunchIntent() {
        val postId = intent.getStringExtra(EXTRA_POST_ID)
        when {
            postId != null -> openDetail(postId)
            intent.getBooleanExtra(EXTRA_OPEN_ACTIVITY, false) -> openActivityTab()
        }
    }

    private fun askNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (AppSettings.notificationPermissionAsked(this)) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            AppSettings.markNotificationPermissionAsked(this)
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            AppSettings.markNotificationPermissionAsked(this)
        }
    }

    private fun registerFcmTokenIfSignedIn() {
        lifecycleScope.launch {
            runCatching {
                (application as SixBlockApplication).container.authRepository.registerFcmToken()
            }
        }
    }

    private fun observeUnreadActivityBadge() {
        val repository = (application as SixBlockApplication).container.notificationRepository
        lifecycleScope.launch {
            repository.observeNotifications().collectLatest { resource ->
                val count = when (resource) {
                    is Resource.Success -> resource.data.count { !it.read }
                    else -> 0
                }
                if (resource is Resource.Success) {
                    showIncomingNotificationIfNeeded(resource.data)
                }
                setBottomBadge(R.id.nav_activity, count)
                (supportFragmentManager.findFragmentByTag(tags.getValue(R.id.nav_feed)) as? FeedFragment)
                    ?.setActivityBadge(count)
            }
        }
    }

    private fun observeFeedUpdateBadge() {
        val container = (application as SixBlockApplication).container
        lifecycleScope.launch {
            container.postRepository
                .observeNearbyPosts(TorontoDefaults.center, 50, category = null)
                .collectLatest { resource ->
                    val count = when (resource) {
                        is Resource.Success -> {
                            val seenAt = AppSettings.feedSeenAt(this@MainActivity)
                            resource.data.count { it.createdAt > seenAt }
                        }
                        else -> 0
                    }
                    val visibleFeed = binding.bottomNav.selectedItemId == R.id.nav_feed
                    setBottomBadge(R.id.nav_feed, if (visibleFeed) 0 else count)
                }
        }
    }

    private fun onRootTabOpened(itemId: Int) {
        when (itemId) {
            R.id.nav_feed -> {
                AppSettings.markFeedSeenNow(this)
                binding.bottomNav.removeBadge(R.id.nav_feed)
            }
            R.id.nav_activity -> {
                binding.bottomNav.removeBadge(R.id.nav_activity)
                (supportFragmentManager.findFragmentByTag(tags.getValue(R.id.nav_feed)) as? FeedFragment)
                    ?.setActivityBadge(0)
                lifecycleScope.launch {
                    (application as SixBlockApplication).container.notificationRepository.markAllRead()
                }
            }
        }
    }

    private fun setBottomBadge(itemId: Int, count: Int) {
        if (count <= 0) {
            binding.bottomNav.removeBadge(itemId)
            return
        }
        binding.bottomNav.getOrCreateBadge(itemId).apply {
            number = count.coerceAtMost(99)
            isVisible = true
        }
    }

    private fun showIncomingNotificationIfNeeded(notifications: List<NotificationItem>) {
        if (binding.bottomNav.selectedItemId == R.id.nav_activity) return
        val lastShownAt = AppSettings.activityNotificationShownAt(this)
        val newestUnread = notifications
            .asSequence()
            .filter { !it.read && it.createdAt > lastShownAt }
            .maxByOrNull { it.createdAt }
            ?: return
        SixBlockNotificationPresenter.showActivityNotification(this, newestUnread)
        AppSettings.markActivityNotificationShown(this, newestUnread.createdAt)
    }

    companion object {
        const val EXTRA_POST_ID = "postId"
        const val EXTRA_OPEN_ACTIVITY = "openActivity"
    }
}
