package com.sixblock.app.ui.activity

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sixblock.app.core.util.TimeAgoFormatter
import com.sixblock.app.databinding.ItemActivityBinding
import com.sixblock.app.domain.model.NotificationItem

class ActivityAdapter(
    private val onClick: (NotificationItem) -> Unit
) : ListAdapter<NotificationItem, ActivityAdapter.ActivityViewHolder>(Diff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        return ActivityViewHolder(
            binding = ItemActivityBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onClick = onClick
        )
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ActivityViewHolder(
        private val binding: ItemActivityBinding,
        private val onClick: (NotificationItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: NotificationItem) {
            binding.activityTitleText.text = item.title
            binding.activityBodyText.text = "${item.body} - ${TimeAgoFormatter.format(item.createdAt)}"
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<NotificationItem>() {
        override fun areItemsTheSame(oldItem: NotificationItem, newItem: NotificationItem): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: NotificationItem, newItem: NotificationItem): Boolean = oldItem == newItem
    }
}
