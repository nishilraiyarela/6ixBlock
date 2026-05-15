package com.sixblock.app.ui.saved

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.sixblock.app.databinding.FragmentSavedPostsBinding
import com.sixblock.app.domain.model.CommunityPost
import com.sixblock.app.ui.common.sixBlockFactory
import com.sixblock.app.ui.feed.PostAdapter
import com.sixblock.app.ui.main.MainActivity

class SavedPostsFragment : Fragment() {
    private var _binding: FragmentSavedPostsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SavedPostsViewModel by viewModels { sixBlockFactory }
    private val adapter = PostAdapter(
        onClick = { (requireActivity() as MainActivity).openDetail(it.id) },
        onLike = { viewModel.toggleLike(it) },
        onSave = { viewModel.toggleSave(it) },
        onMore = { anchor, post, _ -> showSavedPostOptions(anchor, post) }
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSavedPostsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.savedBackButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.savedPostsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.savedPostsRecycler.adapter = adapter

        viewModel.currentUserId.observe(viewLifecycleOwner) { userId ->
            adapter.currentUserId = userId
        }
        viewModel.postsState.observe(viewLifecycleOwner) { state ->
            adapter.submitList(state.data.orEmpty())
            binding.savedEmptyState.visibility = if (!state.isLoading && state.data.orEmpty().isEmpty()) View.VISIBLE else View.GONE
            binding.savedEmptyMessage.text = state.errorMessage ?: state.emptyMessage ?: "Saved posts will appear here."
        }
        viewModel.actionMessage.observe(viewLifecycleOwner) { message ->
            if (message != null) Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
        viewModel.load()
    }

    private fun showSavedPostOptions(anchor: View, post: CommunityPost) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add("Share")
        popup.menu.add("Remove from saved")
        popup.setOnMenuItemClickListener { item ->
            when (item.title.toString()) {
                "Share" -> sharePost(post)
                "Remove from saved" -> viewModel.toggleSave(post)
            }
            true
        }
        popup.show()
    }

    private fun sharePost(post: CommunityPost) {
        val shareText = "${post.title}\n\n${post.body}\n\n6ixBlock post: https://sixblock.app/posts/${post.id}"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, post.title)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, "Share post"))
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
