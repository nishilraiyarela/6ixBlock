package com.sixblock.app.ui.feed

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sixblock.app.R
import com.sixblock.app.core.util.GeoUtils
import com.sixblock.app.core.util.TimeAgoFormatter
import com.sixblock.app.databinding.ItemPostBinding
import com.sixblock.app.domain.model.CommunityPost
import com.sixblock.app.domain.model.PostCategory

class PostAdapter(
    private val onClick: (CommunityPost) -> Unit,
    private val onLike: (CommunityPost) -> Unit,
    private val onSave: (CommunityPost) -> Unit,
    private val onMore: (View, CommunityPost, Boolean) -> Unit
) : ListAdapter<CommunityPost, PostAdapter.PostViewHolder>(Diff) {
    var currentUserId: String? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        return PostViewHolder(
            binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onClick = onClick,
            onLike = onLike,
            onSave = onSave,
            onMore = onMore
        )
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position), currentUserId)
    }

    class PostViewHolder(
        private val binding: ItemPostBinding,
        private val onClick: (CommunityPost) -> Unit,
        private val onLike: (CommunityPost) -> Unit,
        private val onSave: (CommunityPost) -> Unit,
        private val onMore: (View, CommunityPost, Boolean) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(post: CommunityPost, currentUserId: String?) {
            binding.categoryIconImage.setImageResource(post.category.iconRes())
            binding.categoryText.text = post.category.label
            binding.distanceText.text = if (post.approximateArea.isNotBlank()) {
                GeoUtils.approximateDistanceLabel(post.distanceKm)
                    .takeIf { it.isNotBlank() }
                    .orEmpty()
            } else {
                ""
            }
            binding.titleText.text = post.title
            binding.bodyText.text = post.body
            binding.bodyText.visibility = if (post.body.isBlank()) View.GONE else View.VISIBLE
            binding.locationText.text = post.approximateArea.ifBlank {
                binding.root.context.getString(R.string.area_not_shared)
            }
            binding.metaText.text = TimeAgoFormatter.format(post.createdAt)
            binding.likeCountText.text = post.likeCount.toString()
            binding.commentCountText.text = post.commentCount.toString()
            binding.likePostButton.setIconResource(
                if (post.likedByCurrentUser) R.drawable.ic_favorite_filled_24 else R.drawable.ic_favorite_24
            )
            binding.savePostButton.setIconResource(
                if (post.savedByCurrentUser) R.drawable.ic_bookmark_filled_24 else R.drawable.ic_bookmark_24
            )

            val likeIconColor = ContextCompat.getColor(
                binding.root.context,
                if (post.likedByCurrentUser) R.color.action_like else R.color.feed_muted
            )
            binding.likePostButton.iconTint = ColorStateList.valueOf(likeIconColor)
            val saveIconColor = ContextCompat.getColor(
                binding.root.context,
                if (post.savedByCurrentUser) R.color.action_saved else R.color.feed_muted
            )
            binding.savePostButton.iconTint = ColorStateList.valueOf(saveIconColor)

            binding.root.setOnClickListener { onClick(post) }
            binding.likePostButton.setOnClickListener { onLike(post) }
            binding.commentPostButton.setOnClickListener { onClick(post) }
            val canManage = currentUserId != null && currentUserId == post.authorId
            binding.savePostButton.setOnClickListener { onSave(post) }
            binding.morePostButton.setOnClickListener { onMore(binding.morePostButton, post, canManage) }
        }

        private fun PostCategory.iconRes(): Int = when (this) {
            PostCategory.LOST_PET -> R.drawable.ic_category_pet_20
            PostCategory.LOCAL_EVENT -> R.drawable.ic_category_event_20
            PostCategory.FREE_STUFF -> R.drawable.ic_category_free_20
            PostCategory.HELP_REQUEST -> R.drawable.ic_category_help_20
            PostCategory.SAFETY_ALERT -> R.drawable.ic_category_alert_20
            PostCategory.RECOMMENDATION -> R.drawable.ic_category_recommendation_20
        }
    }

    private object Diff : DiffUtil.ItemCallback<CommunityPost>() {
        override fun areItemsTheSame(oldItem: CommunityPost, newItem: CommunityPost): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: CommunityPost, newItem: CommunityPost): Boolean = oldItem == newItem
    }
}
