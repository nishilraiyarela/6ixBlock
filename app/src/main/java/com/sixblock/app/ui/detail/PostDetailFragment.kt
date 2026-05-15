package com.sixblock.app.ui.detail

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sixblock.app.databinding.FragmentPostDetailBinding
import com.sixblock.app.domain.model.CommunityPost
import com.sixblock.app.domain.model.PostComment
import com.sixblock.app.ui.common.sixBlockFactory
import com.sixblock.app.ui.main.MainActivity

class PostDetailFragment : Fragment() {
    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PostDetailViewModel by viewModels { sixBlockFactory }
    private val headerAdapter = PostHeaderAdapter(
        onShare = { sharePost(it) },
        onEdit = { (requireActivity() as MainActivity).openCreateForEdit(it) },
        onDelete = { showDeletePostDialog(it) },
        onHide = { viewModel.hidePost(it.id) },
        onReport = { viewModel.reportPost(it.id) }
    )
    private val commentAdapter = CommentAdapter(
        onEdit = { showEditCommentDialog(it) },
        onDelete = { showDeleteCommentDialog(it) }
    )
    private val postId: String by lazy { requireArguments().getString(ARG_POST_ID).orEmpty() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPostDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.commentsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.commentsRecycler.adapter = ConcatAdapter(headerAdapter, commentAdapter)
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.sendCommentButton.setOnClickListener {
            viewModel.addComment(postId, binding.commentInput.text?.toString().orEmpty())
            binding.commentInput.text?.clear()
        }
        viewModel.postState.observe(viewLifecycleOwner) { state ->
            headerAdapter.submitPost(state.data)
            commentAdapter.postOwnerId = state.data?.authorId
            state.errorMessage?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
        }
        viewModel.commentsState.observe(viewLifecycleOwner) { state ->
            commentAdapter.submitList(state.data.orEmpty())
        }
        viewModel.actionMessage.observe(viewLifecycleOwner) { message ->
            if (message != null) Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
        viewModel.currentUserId.observe(viewLifecycleOwner) { userId ->
            commentAdapter.currentUserId = userId
            headerAdapter.currentUserId = userId
        }
        viewModel.postDeleted.observe(viewLifecycleOwner) { deleted ->
            if (deleted) requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        viewModel.load(postId)
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

    private fun showDeletePostDialog(post: CommunityPost) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete post?")
            .setMessage("This removes the post from the nearby board.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deletePost(post.id)
            }
            .show()
    }

    private fun showEditCommentDialog(comment: PostComment) {
        val input = EditText(requireContext()).apply {
            setText(comment.body)
            setSelection(text.length)
            minLines = 2
            maxLines = 5
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit comment")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                viewModel.editComment(postId, comment.id, input.text?.toString().orEmpty())
            }
            .show()
    }

    private fun showDeleteCommentDialog(comment: PostComment) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete comment?")
            .setMessage("This removes the comment from the post.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteComment(postId, comment.id)
            }
            .show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_POST_ID = "post_id"
        fun newInstance(postId: String): PostDetailFragment {
            return PostDetailFragment().apply {
                arguments = Bundle().apply { putString(ARG_POST_ID, postId) }
            }
        }
    }
}
