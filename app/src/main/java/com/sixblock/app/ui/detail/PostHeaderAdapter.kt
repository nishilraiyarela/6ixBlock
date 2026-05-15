package com.sixblock.app.ui.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sixblock.app.core.util.GeoUtils
import com.sixblock.app.core.util.TimeAgoFormatter
import com.sixblock.app.databinding.ItemPostDetailHeaderBinding
import com.sixblock.app.domain.model.CommunityPost

class PostHeaderAdapter(
    private val onShare: (CommunityPost) -> Unit,
    private val onEdit: (CommunityPost) -> Unit,
    private val onDelete: (CommunityPost) -> Unit,
    private val onHide: (CommunityPost) -> Unit,
    private val onReport: (CommunityPost) -> Unit
) : RecyclerView.Adapter<PostHeaderAdapter.HeaderViewHolder>() {
    private var post: CommunityPost? = null
    var currentUserId: String? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun submitPost(newPost: CommunityPost?) {
        post = newPost
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = if (post == null) 0 else 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        return HeaderViewHolder(
            binding = ItemPostDetailHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onShare = onShare,
            onEdit = onEdit,
            onDelete = onDelete,
            onHide = onHide,
            onReport = onReport
        )
    }

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        post?.let { holder.bind(it, currentUserId) }
    }

    class HeaderViewHolder(
        private val binding: ItemPostDetailHeaderBinding,
        private val onShare: (CommunityPost) -> Unit,
        private val onEdit: (CommunityPost) -> Unit,
        private val onDelete: (CommunityPost) -> Unit,
        private val onHide: (CommunityPost) -> Unit,
        private val onReport: (CommunityPost) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(post: CommunityPost, currentUserId: String?) {
            binding.categoryText.text = post.category.label
            val hasPublicLocation = post.approximateArea.isNotBlank()
            binding.distanceText.text = if (hasPublicLocation) GeoUtils.approximateDistanceLabel(post.distanceKm) else ""
            binding.titleText.text = post.title
            binding.bodyText.text = post.body
            val locationPart = post.approximateArea.takeIf { it.isNotBlank() }?.let { " - $it" }.orEmpty()
            binding.metaText.text = "${post.authorName}$locationPart - ${TimeAgoFormatter.format(post.createdAt)}"
            binding.root.setOnLongClickListener {
                showActions(post)
                true
            }
            binding.actionMenuButton.setOnClickListener { showActions(post) }
            binding.sharePostButton.setOnClickListener { onShare(post) }
            val canManage = currentUserId != null && currentUserId == post.authorId
            binding.editPostButton.visibility = if (canManage) View.VISIBLE else View.GONE
            binding.deletePostButton.visibility = if (canManage) View.VISIBLE else View.GONE
            binding.editPostButton.setOnClickListener { onEdit(post) }
            binding.deletePostButton.setOnClickListener { onDelete(post) }
            if (post.imageUrl.isNullOrBlank()) {
                binding.postImage.visibility = View.GONE
            } else {
                binding.postImage.visibility = View.VISIBLE
                Glide.with(binding.postImage).load(post.imageUrl).centerCrop().into(binding.postImage)
            }
        }

        private fun showActions(post: CommunityPost) {
            PopupMenu(binding.root.context, binding.root).apply {
                menu.add("Hide")
                menu.add("Hide and report")
                setOnMenuItemClickListener { item ->
                    when (item.title.toString()) {
                        "Hide" -> onHide(post)
                        "Hide and report" -> onReport(post)
                    }
                    true
                }
            }.show()
        }
    }
}
