package com.sixblock.app.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sixblock.app.R
import com.sixblock.app.databinding.FragmentProfileBinding
import com.sixblock.app.domain.model.CommunityPost
import com.sixblock.app.domain.model.PostCategory
import com.sixblock.app.ui.auth.AuthActivity
import com.sixblock.app.ui.common.sixBlockFactory
import com.sixblock.app.ui.feed.PostAdapter
import com.sixblock.app.ui.main.MainActivity
import com.sixblock.app.ui.settings.SettingsFragment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels { sixBlockFactory }
    private var latestEmail: String = ""
    private var latestArea: String = ""
    private var latestPhotoUrl: String = ""
    private var latestGender: String = ""
    private var selectedTab: ProfileTab = ProfileTab.POSTS
    private var myPosts: List<CommunityPost> = emptyList()
    private var savedPosts: List<CommunityPost> = emptyList()
    private var alerts: List<CommunityPost> = emptyList()
    private val postAdapter = PostAdapter(
        onClick = { (requireActivity() as MainActivity).openDetail(it.id) },
        onLike = { viewModel.toggleLike(it) },
        onSave = { viewModel.toggleSave(it) },
        onMore = { anchor, post, canManage -> showPostOptions(anchor, post, canManage) }
    )
    private val genderOptions = listOf("Male", "Female", "Other", "Prefer not to say")
    private val areaOptions = listOf(
        "Downtown",
        "Scarborough",
        "North York",
        "Etobicoke",
        "York",
        "East York",
        "The Beaches",
        "Leslieville",
        "Liberty Village",
        "Queen West",
        "Danforth",
        "Parkdale",
        "Roncesvalles",
        "Midtown",
        "Other GTA area"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding.profileEmailInput.isEnabled = false
        binding.profileContentRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.profileContentRecycler.setHasFixedSize(false)
        binding.profileContentRecycler.isNestedScrollingEnabled = false
        binding.profileContentRecycler.adapter = postAdapter

        binding.settingsButton.setOnClickListener {
            (requireActivity() as MainActivity).openFullScreen(SettingsFragment(), "settings")
        }
        binding.postsStatButton.setOnClickListener { selectTab(ProfileTab.POSTS) }
        binding.savedStatButton.setOnClickListener { selectTab(ProfileTab.SAVED) }
        binding.alertsStatButton.setOnClickListener { selectTab(ProfileTab.ALERTS) }
        binding.signOutButton.setOnClickListener {
            showSignOutDialog()
        }
        val areaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, areaOptions)
        binding.profileAreaDropdown.setAdapter(areaAdapter)
        binding.profileAreaDropdown.setOnClickListener { binding.profileAreaDropdown.showDropDown() }
        val genderAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, genderOptions)
        binding.profileGenderDropdown.setAdapter(genderAdapter)
        binding.profileGenderDropdown.setOnClickListener { binding.profileGenderDropdown.showDropDown() }
        binding.profilePromptHeader.setOnClickListener {
            toggleProfileForm()
        }
        binding.profileEditButton.setOnClickListener {
            toggleProfileForm()
        }
        binding.profileInfoRequestText.setOnClickListener {
            toggleProfileForm()
        }
        binding.saveProfileInfoButton.setOnClickListener {
            viewModel.updateProfileInfo(
                email = binding.profileEmailInput.text?.toString().orEmpty(),
                photoUrl = latestPhotoUrl,
                neighbourhood = binding.profileAreaDropdown.text?.toString().orEmpty(),
                gender = binding.profileGenderDropdown.text?.toString().orEmpty()
            )
        }
        viewModel.profileState.observe(viewLifecycleOwner) { profile ->
            latestEmail = profile?.email.orEmpty()
            latestArea = profile?.neighbourhood.orEmpty()
            latestPhotoUrl = profile?.photoUrl.orEmpty()
            latestGender = profile?.gender.orEmpty()
            binding.profileNameText.text = "Profile"
            binding.profileEmailText.text = "Stronger together."
            binding.profileTaglineText.text = "Your block, Your people."
            binding.memberSinceText.text = "Member Since: ${formatMemberSince(profile?.createdAt)}"
            binding.profileEmailInput.setText(latestEmail)
            binding.profileAreaDropdown.post {
                binding.profileAreaDropdown.setText(latestArea, false)
            }
            binding.profileGenderDropdown.post {
                binding.profileGenderDropdown.setText(latestGender, false)
            }
            val missingInfo = profile?.email.isNullOrBlank() || profile?.neighbourhood.isNullOrBlank() || profile?.gender.isNullOrBlank()
            if (missingInfo) {
                binding.profileInfoRequestTitle.text = "Complete your profile"
                binding.profileInfoRequestText.text = "Add email, area, and profile details so 6ixBlock can personalize your local experience."
                binding.profileNotificationDot.visibility = View.VISIBLE
            } else {
                binding.profileInfoRequestTitle.text = "About Me"
                binding.profileInfoRequestText.text = listOf(latestArea, latestGender)
                    .filter { it.isNotBlank() }
                    .joinToString(" / ")
                    .ifBlank { "Your local profile details are saved privately for app personalization." }
                binding.profileNotificationDot.visibility = View.GONE
            }
        }
        viewModel.currentUserId.observe(viewLifecycleOwner) { userId ->
            postAdapter.currentUserId = userId
        }
        viewModel.myPostsState.observe(viewLifecycleOwner) { state ->
            myPosts = state.data.orEmpty()
            binding.postsCountText.text = myPosts.size.toString()
            if (selectedTab == ProfileTab.POSTS) renderSelectedTab(state.emptyMessage ?: "You haven't posted anything yet.")
        }
        viewModel.savedPostsState.observe(viewLifecycleOwner) { state ->
            savedPosts = state.data.orEmpty()
            binding.savedCountText.text = savedPosts.size.toString()
            if (selectedTab == ProfileTab.SAVED) renderSelectedTab(state.emptyMessage ?: "Saved posts will appear here.")
        }
        viewModel.alertsState.observe(viewLifecycleOwner) { state ->
            alerts = state.data.orEmpty()
            binding.alertsCountText.text = alerts.size.toString()
            if (selectedTab == ProfileTab.ALERTS) renderSelectedTab(state.emptyMessage ?: "Your alerts will appear here.")
        }
        viewModel.message.observe(viewLifecycleOwner) { message ->
            if (message != null) Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
        viewModel.load()
        selectTab(ProfileTab.POSTS)
    }

    private fun toggleProfileForm() {
        val opening = binding.profileFormContainer.visibility != View.VISIBLE
        binding.profileFormContainer.animate().cancel()
        if (opening) {
            binding.profileAreaDropdown.setText(latestArea, false)
            binding.profileGenderDropdown.setText(latestGender, false)
            binding.profileFormContainer.visibility = View.VISIBLE
            binding.profileFormContainer.alpha = 0f
            binding.profileFormContainer.animate().alpha(1f).setDuration(180L).start()
        } else {
            binding.profileFormContainer.animate()
                .alpha(0f)
                .setDuration(160L)
                .withEndAction { binding.profileFormContainer.visibility = View.GONE }
                .start()
        }
    }

    private fun showSignOutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sign out?")
            .setMessage("You can log back in anytime with this account.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Sign out") { _, _ ->
                viewModel.signOut()
                startActivity(Intent(requireContext(), AuthActivity::class.java))
                requireActivity().finish()
            }
            .show()
    }

    private fun selectTab(tab: ProfileTab) {
        selectedTab = tab
        styleStat(binding.postsStatButton, tab == ProfileTab.POSTS)
        styleStat(binding.savedStatButton, tab == ProfileTab.SAVED)
        styleStat(binding.alertsStatButton, tab == ProfileTab.ALERTS)
        renderSelectedTab()
    }

    private fun styleStat(view: View, selected: Boolean) {
        view.background = ContextCompat.getDrawable(
            requireContext(),
            if (selected) R.drawable.bg_profile_tab_selected else R.drawable.bg_profile_tab_default
        )
        updateStatTextColors(view, selected)
        view.animate()
            .alpha(if (selected) 1f else 0.92f)
            .setDuration(140L)
            .start()
    }

    private fun updateStatTextColors(view: View, selected: Boolean) {
        val color = ContextCompat.getColor(
            requireContext(),
            if (selected) R.color.feed_chip_selected_text else R.color.feed_muted
        )
        if (view is android.widget.TextView) {
            view.setTextColor(color)
        } else if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                updateStatTextColors(view.getChildAt(index), selected)
            }
        }
    }

    private fun renderSelectedTab(emptyOverride: String? = null) {
        val posts = when (selectedTab) {
            ProfileTab.POSTS -> myPosts
            ProfileTab.SAVED -> savedPosts
            ProfileTab.ALERTS -> alerts
        }
        postAdapter.submitList(posts.toList()) {
            _binding?.profileContentRecycler?.requestLayout()
            _binding?.profileContentRecycler?.invalidate()
        }
        val emptyMessage = emptyOverride ?: when (selectedTab) {
            ProfileTab.POSTS -> "You haven't posted anything yet."
            ProfileTab.SAVED -> "Saved posts will appear here."
            ProfileTab.ALERTS -> "Your alerts will appear here."
        }
        binding.profileContentEmpty.text = emptyMessage
        binding.profileContentEmpty.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
        binding.profileContentRecycler.visibility = if (posts.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showPostOptions(anchor: View, post: CommunityPost, canManage: Boolean) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add("Open")
        if (selectedTab == ProfileTab.SAVED) popup.menu.add("Remove from saved")
        if (post.category == PostCategory.SAFETY_ALERT) popup.menu.add("View alert")
        popup.setOnMenuItemClickListener { item ->
            when (item.title.toString()) {
                "Remove from saved" -> viewModel.toggleSave(post)
                else -> (requireActivity() as MainActivity).openDetail(post.id)
            }
            true
        }
        popup.show()
    }

    private fun formatMemberSince(createdAt: Long?): String {
        val timestamp = createdAt ?: return "Today"
        return SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private enum class ProfileTab {
        POSTS,
        SAVED,
        ALERTS
    }
}
