package com.sixblock.app.ui.feed

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sixblock.app.R
import com.sixblock.app.core.util.AppSettings
import com.sixblock.app.databinding.FragmentFeedBinding
import com.sixblock.app.domain.model.CommunityPost
import com.sixblock.app.domain.model.PostCategory
import com.sixblock.app.ui.common.sixBlockFactory
import com.sixblock.app.ui.main.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FeedFragment : Fragment() {
    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FeedViewModel by viewModels { sixBlockFactory }
    private val adapter = PostAdapter(
        onClick = { (requireActivity() as MainActivity).openDetail(it.id) },
        onLike = { viewModel.toggleLike(it) },
        onSave = { viewModel.toggleSave(it) },
        onMore = { anchor, post, canManage -> showPostOptions(anchor, post, canManage) }
    )
    private var selectedCategoryButtonId: Int = R.id.chipAll
    private var lastCategoryIndex: Int = 0
    private var pendingSlideDirection: Int = 1
    private var firstListRender = true
    private var currentSearchQuery: String = ""
    private var latestPosts: List<CommunityPost> = emptyList()
    private var bannerUserDragging = false
    private var bannerPageCallback: ViewPager2.OnPageChangeCallback? = null
    private lateinit var categoryButtons: List<TextView>
    private val bannerImages = listOf(
        R.drawable.feed_banner_1,
        R.drawable.feed_banner_2,
        R.drawable.feed_banner_3
    )
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refresh(radius = currentRadiusKm(), forceFreshLocation = true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.postsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.postsRecycler.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh(radius = currentRadiusKm()) }
        binding.feedActivityButton.setOnClickListener { (requireActivity() as MainActivity).openActivityTab() }
        binding.feedProfileButton.setOnClickListener { (requireActivity() as MainActivity).openProfileTab() }
        setupSearch()
        setupCategoryMenu()
        setupCategorySwipe()
        setupBannerCarousel()
        ensureLocationPermission()
        viewModel.postsState.observe(viewLifecycleOwner) { state ->
            binding.swipeRefresh.isRefreshing = state.isLoading
            renderSkeleton(state.isLoading)
            if (!state.isLoading) {
                latestPosts = state.data.orEmpty()
                renderPosts(filteredPosts())
            } else if (adapter.currentList.isEmpty() && state.data.orEmpty().isNotEmpty()) {
                latestPosts = state.data.orEmpty()
                adapter.submitList(filteredPosts())
            }
            val visiblePosts = filteredPosts()
            binding.emptyState.visibility = if (!state.isLoading && visiblePosts.isEmpty()) View.VISIBLE else View.GONE
            binding.emptyMessage.text = state.errorMessage
                ?: if (currentSearchQuery.isNotBlank()) getString(R.string.feed_empty_search_message, currentSearchQuery)
                else state.emptyMessage ?: getString(R.string.feed_empty_message)
        }
        viewModel.currentUserId.observe(viewLifecycleOwner) { userId ->
            adapter.currentUserId = userId
        }
        viewModel.locationTitle.observe(viewLifecycleOwner) { title ->
            binding.feedLocationText.text = title
        }
        viewModel.actionMessage.observe(viewLifecycleOwner) { message ->
            if (message != null) Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderSkeleton(isLoading: Boolean) {
        val showSkeleton = isLoading && adapter.currentList.isEmpty()
        binding.feedSkeletonState.visibility = if (showSkeleton) View.VISIBLE else View.GONE
        binding.postsRecycler.visibility = if (showSkeleton) View.INVISIBLE else View.VISIBLE
        if (showSkeleton) {
            binding.feedSkeletonState.animate()
                .alpha(0.45f)
                .setDuration(520L)
                .withEndAction {
                    if (_binding != null && binding.feedSkeletonState.visibility == View.VISIBLE) {
                        binding.feedSkeletonState.animate().alpha(1f).setDuration(520L).start()
                    }
                }
                .start()
        } else {
            binding.feedSkeletonState.animate().cancel()
            binding.feedSkeletonState.alpha = 1f
        }
    }

    private fun ensureLocationPermission() {
        val fine = checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s?.toString().orEmpty().trim()
                firstListRender = true
                renderPosts(filteredPosts())
                binding.emptyState.visibility = if (filteredPosts().isEmpty()) View.VISIBLE else View.GONE
                binding.emptyMessage.text = if (currentSearchQuery.isBlank()) {
                    getString(R.string.feed_empty_message)
                } else {
                    getString(R.string.feed_empty_search_message, currentSearchQuery)
                }
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun setupCategoryMenu() {
        categoryButtons = listOf(
            binding.chipAll,
            binding.chipLostPets,
            binding.chipEvents,
            binding.chipFree,
            binding.chipAlerts
        )
        categoryButtons.forEach { button ->
            button.setOnClickListener {
                selectCategory(categoryButtons.indexOf(button))
            }
        }
        styleCategoryButtons(categoryButtons)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupCategorySwipe() {
        var downX = 0f
        var downY = 0f
        val listener = View.OnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                }
                MotionEvent.ACTION_UP -> {
                    val dx = event.x - downX
                    val dy = event.y - downY
                    if (kotlin.math.abs(dx) > 90f && kotlin.math.abs(dx) > kotlin.math.abs(dy) * 1.3f) {
                        if (dx < 0f) selectCategory(lastCategoryIndex + 1)
                        else selectCategory(lastCategoryIndex - 1)
                    }
                }
            }
            false
        }
        binding.postsRecycler.setOnTouchListener(listener)
        binding.emptyState.setOnTouchListener(listener)
    }

    private fun selectCategory(index: Int) {
        if (!::categoryButtons.isInitialized) return
        val safeIndex = index.coerceIn(0, categoryButtons.lastIndex)
        if (safeIndex == lastCategoryIndex && selectedCategoryButtonId == categoryButtons[safeIndex].id) return
        val button = categoryButtons[safeIndex]
        pendingSlideDirection = if (safeIndex >= lastCategoryIndex) 1 else -1
        lastCategoryIndex = safeIndex
        selectedCategoryButtonId = button.id
        styleCategoryButtons(categoryButtons)
        smoothScrollCategoryIntoView(button)
        viewModel.refresh(categoryForChip(button.id), currentRadiusKm())
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) viewModel.refresh(radius = currentRadiusKm())
    }

    private fun currentRadiusKm(): Int = AppSettings.radiusKm(requireContext())

    private fun styleCategoryButtons(buttons: List<TextView>) {
        val selectedText = ContextCompat.getColor(requireContext(), R.color.feed_chip_selected_text)
        val defaultText = ContextCompat.getColor(requireContext(), R.color.feed_muted)
        val selectedBackground = ContextCompat.getColor(requireContext(), R.color.feed_chip_selected)
        val defaultBackground = ContextCompat.getColor(requireContext(), android.R.color.transparent)
        val stroke = ContextCompat.getColor(requireContext(), R.color.feed_line)
        buttons.forEach { button ->
            val selected = button.id == selectedCategoryButtonId
            button.setTextColor(if (selected) selectedText else defaultText)
            button.typeface = Typeface.DEFAULT_BOLD
            (button as? MaterialButton)?.let { materialButton ->
                materialButton.backgroundTintList = ColorStateList.valueOf(if (selected) selectedBackground else defaultBackground)
                materialButton.strokeColor = ColorStateList.valueOf(stroke)
                materialButton.iconTint = ColorStateList.valueOf(if (selected) selectedText else defaultText)
            }
            button.animate()
                .scaleX(if (selected) 1.04f else 1f)
                .scaleY(if (selected) 1.04f else 1f)
                .setDuration(160L)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun smoothScrollCategoryIntoView(button: TextView) {
        val targetScroll = (button.left - binding.categoryRail.width / 2 + button.width / 2).coerceAtLeast(0)
        binding.categoryRail.smoothScrollTo(targetScroll, 0)
    }

    private fun filteredPosts(): List<CommunityPost> {
        val query = currentSearchQuery.lowercase()
        if (query.isBlank()) return latestPosts
        return latestPosts.filter { post ->
            post.title.lowercase().contains(query) ||
                post.body.lowercase().contains(query) ||
                post.authorName.lowercase().contains(query) ||
                post.approximateArea.lowercase().contains(query) ||
                post.category.label.lowercase().contains(query)
        }
    }

    private fun setupBannerCarousel() {
        binding.feedBannerPager.adapter = FeedBannerAdapter(bannerImages) { position ->
            when (position) {
                0 -> (requireActivity() as MainActivity).openCreateTab()
                1 -> (requireActivity() as MainActivity).openMapTab()
                2 -> (requireActivity() as MainActivity).openActivityTab()
            }
        }
        binding.feedBannerPager.offscreenPageLimit = bannerImages.size
        binding.feedBannerPager.setPageTransformer { page, position ->
            val distance = kotlin.math.abs(position).coerceAtMost(1f)
            page.alpha = 0.82f + (1f - distance) * 0.18f
            page.scaleY = 0.96f + (1f - distance) * 0.04f
        }

        renderBannerDots(activeIndex = 0)
        val callback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                renderBannerDots(position)
            }

            override fun onPageScrollStateChanged(state: Int) {
                bannerUserDragging = state == ViewPager2.SCROLL_STATE_DRAGGING
            }
        }
        binding.feedBannerPager.registerOnPageChangeCallback(callback)
        bannerPageCallback = callback

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(BANNER_AUTO_SCROLL_MS)
                    if (_binding != null && !bannerUserDragging && bannerImages.size > 1) {
                        val next = (binding.feedBannerPager.currentItem + 1) % bannerImages.size
                        binding.feedBannerPager.setCurrentItem(next, true)
                    }
                }
            }
        }
    }

    private fun renderBannerDots(activeIndex: Int) {
        binding.feedBannerDots.removeAllViews()
        bannerImages.forEachIndexed { index, _ ->
            val dot = View(requireContext()).apply {
                background = ContextCompat.getDrawable(
                    requireContext(),
                    if (index == activeIndex) R.drawable.bg_banner_dot_active else R.drawable.bg_banner_dot_inactive
                )
            }
            val size = if (index == activeIndex) 8.dp() else 7.dp()
            dot.layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginStart = 4.dp()
                marginEnd = 4.dp()
            }
            binding.feedBannerDots.addView(dot)
        }
    }

    private fun renderPosts(posts: List<CommunityPost>) {
        val incomingIds = posts.map { it.id }
        val currentIds = adapter.currentList.map { it.id }
        if (adapter.currentList == posts && !firstListRender) return
        if (incomingIds == currentIds && !firstListRender) {
            adapter.submitList(posts)
            return
        }
        binding.postsRecycler.animate().cancel()

        if (firstListRender || adapter.currentList.isEmpty()) {
            firstListRender = false
            binding.postsRecycler.alpha = 1f
            binding.postsRecycler.translationX = 0f
            adapter.submitList(posts)
            return
        }

        val slideOut = -28f * pendingSlideDirection
        val slideIn = 32f * pendingSlideDirection
        binding.postsRecycler.animate()
            .alpha(0.25f)
            .translationX(slideOut)
            .setDuration(120L)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                binding.postsRecycler.translationX = slideIn
                adapter.submitList(posts) {
                    binding.postsRecycler.animate()
                        .alpha(1f)
                        .translationX(0f)
                        .setDuration(210L)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }
            }
            .start()
    }

    private fun categoryForChip(id: Int?): PostCategory? = when (id) {
        R.id.chipLostPets -> PostCategory.LOST_PET
        R.id.chipEvents -> PostCategory.LOCAL_EVENT
        R.id.chipFree -> PostCategory.FREE_STUFF
        R.id.chipAlerts -> PostCategory.SAFETY_ALERT
        else -> null
    }

    private fun sharePost(post: CommunityPost) {
        val shareText = "${post.title}\n\n${post.body}\n\n6ixBlock post: https://sixblock.app/posts/${post.id}"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, post.title)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_post_chooser_title)))
    }

    fun setActivityBadge(count: Int) {
        if (_binding == null) return
        binding.feedActivityBadge.visibility = if (count > 0) View.VISIBLE else View.GONE
        binding.feedActivityBadge.text = count.coerceAtMost(99).toString()
    }

    private fun showPostOptions(anchor: View, post: CommunityPost, canManage: Boolean) {
        val popup = PopupMenu(requireContext(), anchor)
        if (canManage) {
            popup.menu.add(0, MENU_EDIT, 0, getString(R.string.menu_edit))
            popup.menu.add(0, MENU_DELETE, 1, getString(R.string.delete))
        }
        popup.menu.add(0, MENU_SHARE, 2, getString(R.string.menu_share))
        popup.menu.add(0, MENU_REPORT, 3, getString(R.string.menu_report))
        popup.menu.add(0, MENU_HIDE, 4, getString(R.string.menu_hide))
        popup.menu.add(0, MENU_REPORT_HIDE, 5, getString(R.string.menu_report_and_hide))
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_EDIT -> (requireActivity() as MainActivity).openCreateForEdit(post)
                MENU_DELETE -> showDeletePostDialog(post)
                MENU_SHARE -> sharePost(post)
                MENU_REPORT -> viewModel.reportPost(post.id, hideAfter = false)
                MENU_HIDE -> viewModel.hidePost(post.id)
                MENU_REPORT_HIDE -> viewModel.reportPost(post.id, hideAfter = true)
            }
            true
        }
        popup.show()
    }

    private fun showDeletePostDialog(post: CommunityPost) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_post_title)
            .setMessage(R.string.delete_post_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deletePost(post.id)
            }
            .show()
    }

    override fun onDestroyView() {
        bannerPageCallback?.let { callback ->
            binding.feedBannerPager.unregisterOnPageChangeCallback(callback)
        }
        bannerPageCallback = null
        _binding = null
        super.onDestroyView()
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    private companion object {
        const val BANNER_AUTO_SCROLL_MS = 3_000L
        const val MENU_EDIT = 1
        const val MENU_DELETE = 2
        const val MENU_SHARE = 3
        const val MENU_REPORT = 4
        const val MENU_HIDE = 5
        const val MENU_REPORT_HIDE = 6
    }
}
