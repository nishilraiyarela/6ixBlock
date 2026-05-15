package com.sixblock.app.ui.feed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sixblock.app.databinding.ItemFeedBannerBinding

class FeedBannerAdapter(
    private val banners: List<Int>,
    private val onBannerClick: (Int) -> Unit
) : RecyclerView.Adapter<FeedBannerAdapter.BannerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val binding = ItemFeedBannerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BannerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        holder.bind(banners[position], position, onBannerClick)
    }

    override fun getItemCount(): Int = banners.size

    class BannerViewHolder(
        private val binding: ItemFeedBannerBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(imageRes: Int, position: Int, onBannerClick: (Int) -> Unit) {
            binding.bannerImage.setImageResource(imageRes)
            binding.root.setOnClickListener { onBannerClick(position) }
        }
    }
}
