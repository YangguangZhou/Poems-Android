package com.jerryz.poems.ui.ai

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jerryz.poems.R
import com.google.android.material.color.MaterialColors
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.UpdateAppearance
import android.animation.Animator
import android.animation.AnimatorListenerAdapter

class TurnAdapter(
    private val onDeleteTurn: (ChatTurn) -> Unit,
    private val onLongPressCopy: (String) -> Unit
) : ListAdapter<ChatTurn, TurnAdapter.TurnVH>(DIFF) {

    companion object {
        data class TurnPayload(
            val userText: String?,
            val assistantText: String?,
            val assistantTyping: Boolean
        )

        val DIFF = object : DiffUtil.ItemCallback<ChatTurn>() {
            override fun areItemsTheSame(oldItem: ChatTurn, newItem: ChatTurn): Boolean {
                val oldId = (oldItem.user?.id ?: "") + "|" + (oldItem.assistant?.id ?: "")
                val newId = (newItem.user?.id ?: "") + "|" + (newItem.assistant?.id ?: "")
                return oldId == newId
            }

            override fun areContentsTheSame(oldItem: ChatTurn, newItem: ChatTurn): Boolean = oldItem == newItem

            override fun getChangePayload(oldItem: ChatTurn, newItem: ChatTurn): Any? {
                val userChanged = (oldItem.user?.content ?: "") != (newItem.user?.content ?: "")
                val asstChanged = (oldItem.assistant?.content ?: "") != (newItem.assistant?.content ?: "")
                if (!userChanged && !asstChanged) return null
                return TurnPayload(
                    userText = if (userChanged) newItem.user?.content else null,
                    assistantText = if (asstChanged) newItem.assistant?.content else null,
                    assistantTyping = newItem.assistant?.content.isNullOrBlank()
                )
            }
        }
    }

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        val item = getItem(position)
        val idStr = (item.user?.id ?: "") + "|" + (item.assistant?.id ?: "")
        return idStr.hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TurnVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_turn, parent, false)
        return TurnVH(v)
    }

    override fun onBindViewHolder(holder: TurnVH, position: Int) {
        holder.bind(getItem(position), onDeleteTurn, onLongPressCopy)
    }

    override fun onBindViewHolder(holder: TurnVH, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            val p = payloads.last() as? TurnPayload
            if (p != null) {
                holder.applyPartial(p, onLongPressCopy)
                return
            }
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    inner class TurnVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userText: TextView = itemView.findViewById(R.id.text_user)
        private val assistantText: TextView = itemView.findViewById(R.id.text_assistant)
        private val typingText: TextView = itemView.findViewById(R.id.text_typing)
        private val deleteButton: View = itemView.findViewById(R.id.button_delete_turn)
        private val handler = Handler(Looper.getMainLooper())
        private var typingRunnable: Runnable? = null
        private var dotCount = 0
        private var highlightAnimator: ValueAnimator? = null

        fun bind(turn: ChatTurn, onDelete: (ChatTurn) -> Unit, onCopy: (String) -> Unit) {
            cancelHighlight()
            // User
            if (turn.user != null) {
                userText.visibility = View.VISIBLE
                userText.text = turn.user.content
                userText.setOnLongClickListener { onCopy(turn.user.content); true }
            } else {
                userText.visibility = View.GONE
            }

            // Assistant
            if (turn.assistant != null) {
                val blank = turn.assistant.content.isBlank()
                if (blank) {
                    assistantText.visibility = View.GONE
                } else {
                    val err = isErrorMessage(turn.assistant.content)
                    assistantText.visibility = View.VISIBLE
                    assistantText.setOnLongClickListener { onCopy(turn.assistant.content); true }
                    if (err) {
                        assistantText.text = turn.assistant.content
                        assistantText.setTextColor(
                            MaterialColors.getColor(itemView.context, com.google.android.material.R.attr.colorError, 0)
                        )
                        cancelHighlight()
                    } else {
                        assistantText.setTextColor(
                            MaterialColors.getColor(itemView.context, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)
                        )
                        renderAssistantText(turn.assistant.content, null)
                    }
                }
                setTyping(blank)
            } else {
                assistantText.visibility = View.GONE
                setTyping(false)
            }

            deleteButton.setOnClickListener { onDelete(turn) }
        }

        fun applyPartial(payload: TurnPayload, onCopy: (String) -> Unit) {
            payload.userText?.let { newText ->
                userText.visibility = View.VISIBLE
                userText.text = newText
                userText.setOnLongClickListener { onCopy(newText); true }
            }
            payload.assistantText?.let { newText ->
                val previousText = assistantText.text?.toString().orEmpty()
                val blank = newText.isBlank()
                if (blank) {
                    assistantText.visibility = View.GONE
                    cancelHighlight()
                } else {
                    assistantText.visibility = View.VISIBLE
                    val err = isErrorMessage(newText)
                    assistantText.setOnLongClickListener { onCopy(newText); true }
                    if (err) {
                        cancelHighlight()
                        assistantText.text = newText
                        assistantText.setTextColor(
                            MaterialColors.getColor(itemView.context, com.google.android.material.R.attr.colorError, 0)
                        )
                    } else {
                        assistantText.setTextColor(
                            MaterialColors.getColor(itemView.context, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)
                        )
                        val highlightStart = if (newText.length > previousText.length) previousText.length else null
                        renderAssistantText(newText, highlightStart)
                    }
                }
                setTyping(blank)
            }
        }

        private fun setTyping(active: Boolean) {
            if (active) {
                typingText.visibility = View.VISIBLE
                startTyping()
            } else {
                stopTyping()
                typingText.visibility = View.GONE
            }
        }

        private fun startTyping() {
            stopTyping()
            dotCount = 0
            val sequence = arrayOf("·", "··", "···")
            typingRunnable = object : Runnable {
                override fun run() {
                    dotCount = (dotCount + 1) % 3
                    typingText.text = sequence[dotCount]
                    handler.postDelayed(this, 450)
                }
            }
            handler.post(typingRunnable!!)
        }

        fun stopTyping() {
            typingRunnable?.let { handler.removeCallbacks(it) }
            typingRunnable = null
        }

        private fun renderAssistantText(text: String, highlightStart: Int?) {
            cancelHighlight()
            if (highlightStart != null && highlightStart < text.length) {
                val spannable = SpannableStringBuilder(text)
                val startColor = MaterialColors.getColor(
                    itemView.context,
                    com.google.android.material.R.attr.colorSecondary,
                    0
                )
                val endColor = MaterialColors.getColor(
                    itemView.context,
                    com.google.android.material.R.attr.colorOnSurfaceVariant,
                    0
                )
                val span = AnimatedColorSpan(startColor)
                spannable.setSpan(span, highlightStart, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                assistantText.text = spannable
                highlightAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 600 // 动画时长稍长，更柔和
                    addUpdateListener {
                        val fraction = it.animatedValue as Float
                        val color = (ArgbEvaluator().evaluate(fraction, startColor, endColor) as Int)
                        span.setColor(color)
                        assistantText.invalidate()
                    }
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            if (assistantText.text is Spanned) {
                                assistantText.text = assistantText.text.toString()
                            }
                            highlightAnimator = null
                        }
                    })
                    start()
                }
            } else {
                assistantText.text = text
            }
        }

        fun cancelHighlight() {
            highlightAnimator?.cancel()
            highlightAnimator = null
            if (assistantText.text is Spanned) {
                assistantText.text = assistantText.text.toString()
            }
        }

        private inner class AnimatedColorSpan(initialColor: Int) : CharacterStyle(), UpdateAppearance {
            private var currentColor: Int = initialColor
            fun setColor(color: Int) {
                currentColor = color
            }
            override fun updateDrawState(tp: TextPaint) {
                tp.color = currentColor
            }
        }
    }

    private fun isErrorMessage(text: String): Boolean {
        val t = text.trim()
        return t.startsWith("请求失败") || t.startsWith("API error") || t.startsWith("Error")
    }

    override fun onViewRecycled(holder: TurnVH) {
        super.onViewRecycled(holder)
        holder.stopTyping()
        holder.cancelHighlight()
    }
}
