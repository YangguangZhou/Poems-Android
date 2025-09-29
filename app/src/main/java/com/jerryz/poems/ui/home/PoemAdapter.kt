package com.jerryz.poems.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.jerryz.poems.R
import com.jerryz.poems.data.Poem
import com.jerryz.poems.databinding.ItemPoemBinding
import com.jerryz.poems.util.AnimationUtils

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
            // 为卡片添加触摸反馈效果
            binding.root.setOnClickListener { view ->
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    AnimationUtils.animateButtonWithHaptic(view) {
                        val poem = getItem(position)
                        // 设置共享元素过渡名称
                        androidx.core.view.ViewCompat.setTransitionName(binding.root, "poem_card_${poem.id}")
                        onItemClick(poem)
                    }
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

            // 设置共享元素过渡名称，确保每个卡片都有唯一标识
            androidx.core.view.ViewCompat.setTransitionName(binding.root, "poem_card_${poem.id}")

            // 优化标签显示，避免跳动 - 使用更稳定的标签更新策略
            val tagsToShow = poem.tags.take(3)
            
            // 只有在标签内容真正改变时才重新设置
            val currentTags = (0 until binding.chipGroupTags.childCount).map { 
                (binding.chipGroupTags.getChildAt(it) as? Chip)?.text?.toString() ?: ""
            }
            
            if (currentTags != tagsToShow) {
                // 使用更平滑的更新方式，先设置为不可见再更新
                binding.chipGroupTags.alpha = 0f
                binding.chipGroupTags.removeAllViews()
                
                tagsToShow.forEach { tag ->
                    val chip = Chip(binding.root.context).apply {
                        text = tag
                        isCheckable = false
                        isClickable = false
                        chipStrokeWidth = 0f

                        // 使用主题适配的颜色
                        val bg = com.google.android.material.color.MaterialColors.getColor(
                            context, 
                            com.google.android.material.R.attr.colorSecondaryContainer, 
                            0
                        )
                        val fg = com.google.android.material.color.MaterialColors.getColor(
                            context, 
                            com.google.android.material.R.attr.colorOnSecondaryContainer, 
                            0
                        )
                        chipBackgroundColor = android.content.res.ColorStateList.valueOf(bg)
                        setTextColor(fg)

                        textSize = 12f
                    }
                    binding.chipGroupTags.addView(chip)
                }
                
                // 添加完成后淡入显示，减少视觉跳动
                binding.chipGroupTags.animate()
                    .alpha(1f)
                    .setDuration(150)
                    .start()
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