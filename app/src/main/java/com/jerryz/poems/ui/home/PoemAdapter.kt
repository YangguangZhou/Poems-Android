package com.jerryz.poems.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.jerryz.poems.data.Poem
import com.jerryz.poems.databinding.ItemPoemBinding

class PoemAdapter(private val onItemClick: (Poem) -> Unit) :
    ListAdapter<Poem, PoemAdapter.PoemViewHolder>(PoemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PoemViewHolder {
        val binding = ItemPoemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PoemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PoemViewHolder, position: Int) {
        val poem = getItem(position)
        holder.bind(poem)
    }

    inner class PoemViewHolder(private val binding: ItemPoemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(poem: Poem) {
            binding.textTitle.text = poem.title
            binding.textAuthor.text = poem.author

            // 设置内容预览（最多显示前两行）
            val contentPreview = poem.content.joinToString("\n", limit = 2)
            binding.textPreview.text = contentPreview

            // 设置收藏图标
            binding.iconFavorite.visibility = if (poem.isFavorite) View.VISIBLE else View.GONE

            // 显示标签
            binding.chipGroupTags.removeAllViews()

            // 最多显示3个标签，避免过多标签导致UI混乱
            val tagsToShow = poem.tags.take(3)

            tagsToShow.forEach { tag ->
                val chip = Chip(binding.root.context).apply {
                    text = tag
                    isCheckable = false
                    isClickable = false
                    chipStrokeWidth = 0f

                    // 使用更明显的背景色，采用 secondaryContainer 颜色
                    setChipBackgroundColorResource(com.google.android.material.R.color.m3_sys_color_dynamic_light_secondary_container)
                    // 使用匹配的文本颜色确保可读性
                    setTextColor(context.getColor(com.google.android.material.R.color.m3_sys_color_dynamic_light_on_secondary_container))

                    textSize = 12f
                }
                binding.chipGroupTags.addView(chip)
            }
        }
    }

    class PoemDiffCallback : DiffUtil.ItemCallback<Poem>() {
        override fun areItemsTheSame(oldItem: Poem, newItem: Poem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Poem, newItem: Poem): Boolean {
            return oldItem == newItem
        }
    }
}