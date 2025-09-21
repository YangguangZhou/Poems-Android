package com.jerryz.poems.ui.ai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jerryz.poems.R

class ChatFooterAdapter : RecyclerView.Adapter<ChatFooterAdapter.VH>() {
    class VH(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_footer, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = 1

    override fun onBindViewHolder(holder: VH, position: Int) {}
}

