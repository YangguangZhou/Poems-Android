package com.jerryz.poems.ui.ai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.jerryz.poems.R

class DictationFooterAdapter(
    private val onGenerate: () -> Unit
) : RecyclerView.Adapter<DictationFooterAdapter.VH>() {

    private var loading: Boolean = false
    private var hasItems: Boolean = false

    fun setState(loading: Boolean, hasItems: Boolean) {
        this.loading = loading
        this.hasItems = hasItems
        notifyItemChanged(0)
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val btn: MaterialButton = itemView.findViewById(R.id.button_generate_footer)
        val progress: CircularProgressIndicator = itemView.findViewById(R.id.progress_footer)
        val disclaimer: TextView = itemView.findViewById(R.id.text_disclaimer_footer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_dictation_footer, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = 1

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.btn.text = holder.itemView.context.getString(
            if (hasItems) R.string.generate_another_set else R.string.create_set
        )
        holder.btn.isEnabled = !loading
        holder.progress.visibility = if (loading) View.VISIBLE else View.GONE
        holder.btn.setOnClickListener { if (!loading) onGenerate() }
        // disclaimer text set via layout
    }
}

