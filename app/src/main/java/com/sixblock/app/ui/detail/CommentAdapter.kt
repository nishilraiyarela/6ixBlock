package com.sixblock.app.ui.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sixblock.app.core.util.TimeAgoFormatter
import com.sixblock.app.databinding.ItemCommentBinding
import com.sixblock.app.domain.model.PostComment

class CommentAdapter(
    private val onEdit: (PostComment) -> Unit,
    private val onDelete: (PostComment) -> Unit
) : ListAdapter<PostComment, CommentAdapter.CommentViewHolder>(Diff) {
    var currentUserId: String? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var postOwnerId: String? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        return CommentViewHolder(
            binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onEdit = onEdit,
            onDelete = onDelete
        )
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position), currentUserId, postOwnerId)
    }

    class CommentViewHolder(
        private val binding: ItemCommentBinding,
        private val onEdit: (PostComment) -> Unit,
        private val onDelete: (PostComment) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(comment: PostComment, currentUserId: String?, postOwnerId: String?) {
            val editedLabel = if (comment.edited) " - edited" else ""
            binding.commentMetaText.text = "${comment.authorName} - ${TimeAgoFormatter.format(comment.createdAt)}$editedLabel"
            binding.commentBodyText.text = comment.body
            val canEdit = currentUserId != null && currentUserId == comment.authorId
            val canDelete = currentUserId != null && (currentUserId == comment.authorId || currentUserId == postOwnerId)
            binding.commentActionButton.visibility = if (canEdit || canDelete) android.view.View.VISIBLE else android.view.View.GONE
            binding.commentActionButton.setOnClickListener { showMenu(comment, canEdit, canDelete) }
            binding.root.setOnLongClickListener {
                if (canEdit || canDelete) {
                    showMenu(comment, canEdit, canDelete)
                    true
                } else {
                    false
                }
            }
        }

        private fun showMenu(comment: PostComment, canEdit: Boolean, canDelete: Boolean) {
            PopupMenu(binding.root.context, binding.commentActionButton).apply {
                if (canEdit) menu.add("Edit")
                if (canDelete) menu.add("Delete")
                setOnMenuItemClickListener { item ->
                    when (item.title.toString()) {
                        "Edit" -> onEdit(comment)
                        "Delete" -> onDelete(comment)
                    }
                    true
                }
            }.show()
        }
    }

    private object Diff : DiffUtil.ItemCallback<PostComment>() {
        override fun areItemsTheSame(oldItem: PostComment, newItem: PostComment): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: PostComment, newItem: PostComment): Boolean = oldItem == newItem
    }
}
