package com.jerryz.poems.ui.ai

import android.text.Editable
import android.text.TextWatcher
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.textfield.TextInputEditText
import com.jerryz.poems.R

class DictationAdapter(
    private val onTextChanged: (index: Int, text: String) -> Unit,
    private val onCheck: (index: Int) -> Unit,
    private val onCheckResult: (CheckResult?, View) -> Unit
) : ListAdapter<DictationQuestion, DictationAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<DictationQuestion>() {
            override fun areItemsTheSame(oldItem: DictationQuestion, newItem: DictationQuestion): Boolean =
                oldItem.prompt == newItem.prompt && oldItem.answer == newItem.answer

            override fun areContentsTheSame(oldItem: DictationQuestion, newItem: DictationQuestion): Boolean =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_dictation_question, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position), position, onTextChanged, onCheck, onCheckResult)
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textPrompt: TextView = itemView.findViewById(R.id.text_prompt)
        private val textMask: TextView = itemView.findViewById(R.id.text_mask)
        private val input: TextInputEditText = itemView.findViewById(R.id.input_answer)
        private val buttonCheck: MaterialButton = itemView.findViewById(R.id.button_check)
        private val textResult: TextView = itemView.findViewById(R.id.text_result)
        private val textExplanation: TextView = itemView.findViewById(R.id.text_explanation)
        private val textFilled: TextView = itemView.findViewById(R.id.text_filled)
        private val textCorrectAnswer: TextView = itemView.findViewById(R.id.text_correct_answer)

        private var watcher: TextWatcher? = null

        fun bind(
            item: DictationQuestion,
            position: Int,
            onTextChanged: (index: Int, text: String) -> Unit,
            onCheck: (index: Int) -> Unit,
            onCheckResult: (CheckResult?, View) -> Unit
        ) {
            textPrompt.text = item.prompt
            // Show underline mask before checking
            textMask.visibility = View.VISIBLE
            textMask.text = buildBlankMask(item.answer)
            textFilled.visibility = View.GONE

            // Reset watcher
            watcher?.let { input.removeTextChangedListener(it) }
            val current = input.text?.toString() ?: ""
            if (current != item.userInput) {
                input.setText(item.userInput)
                input.setSelection(item.userInput.length)
            }
            watcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    onTextChanged(position, s?.toString() ?: "")
                }
                override fun afterTextChanged(s: Editable?) {}
            }
            input.addTextChangedListener(watcher)

            buttonCheck.setOnClickListener { view ->
                // 检查时添加轻微震动和动画
                com.jerryz.poems.util.AnimationUtils.animateButtonWithHaptic(view) {
                    onCheck(position)
                }
            }

            // Result
            when (val r = item.result) {
                null -> {
                    textResult.visibility = View.GONE
                    textExplanation.visibility = View.GONE
                    textMask.visibility = View.VISIBLE
                    textMask.text = buildBlankMask(item.answer)
                    textFilled.visibility = View.GONE
                    textCorrectAnswer.visibility = View.GONE
                }
                is CheckResult.Correct -> {
                    textResult.visibility = View.VISIBLE
                    textResult.setText(R.string.correct)
                    textResult.setTextColor(
                        MaterialColors.getColor(itemView.context, com.google.android.material.R.attr.colorPrimary, 0)
                    )
                    // 添加成功动画效果
                    com.jerryz.poems.util.AnimationUtils.animateSuccess(textResult)
                    // 触发检查结果回调
                    onCheckResult(r, textResult)
                    textExplanation.visibility = View.VISIBLE
                    textExplanation.text = itemView.context.getString(R.string.explanation) + "：" + item.explanation
                    // Show colored correct answer on underline
                    textMask.visibility = View.VISIBLE
                    textMask.text = buildHighlightedAnswer(item)
                    // No need to show correct answer again below when fully correct
                    textCorrectAnswer.visibility = View.GONE
                }
                is CheckResult.Typos -> {
                    textResult.visibility = View.VISIBLE
                    textResult.text = itemView.context.getString(R.string.typos_count, r.wrongCount)
                    textResult.setTextColor(
                        MaterialColors.getColor(itemView.context, com.google.android.material.R.attr.colorSecondary, 0)
                    )
                    // 添加部分错误动画效果
                    com.jerryz.poems.util.AnimationUtils.animatePartialError(textResult)
                    // 触发检查结果回调
                    onCheckResult(r, textResult)
                    textExplanation.visibility = View.VISIBLE
                    textExplanation.text = itemView.context.getString(R.string.explanation) + "：" + item.explanation
                    // Show colored correct answer on underline; plain answer below
                    textMask.visibility = View.VISIBLE
                    textMask.text = buildHighlightedAnswer(item)
                    textFilled.visibility = View.GONE
                    textCorrectAnswer.visibility = View.VISIBLE
                    textCorrectAnswer.text = itemView.context.getString(R.string.correct_answer) + "：" + item.answer
                }
                is CheckResult.WholeWrong -> {
                    textResult.visibility = View.VISIBLE
                    textResult.setText(R.string.whole_sentence_wrong)
                    textResult.setTextColor(
                        MaterialColors.getColor(itemView.context, com.google.android.material.R.attr.colorError, 0)
                    )
                    // 添加错误动画效果
                    com.jerryz.poems.util.AnimationUtils.animateError(textResult)
                    // 触发检查结果回调
                    onCheckResult(r, textResult)
                    textExplanation.visibility = View.VISIBLE
                    textExplanation.text = itemView.context.getString(R.string.explanation) + "：" + item.explanation
                    // Show colored correct answer on underline; plain answer below
                    textMask.visibility = View.VISIBLE
                    textMask.text = buildHighlightedAnswer(item)
                    textFilled.visibility = View.GONE
                    textCorrectAnswer.visibility = View.VISIBLE
                    textCorrectAnswer.text = itemView.context.getString(R.string.correct_answer) + "：" + item.answer
                }
            }
        }

        private fun buildBlankMask(answer: String): String {
            val sb = StringBuilder()
            for (ch in answer) {
                when {
                    ch.isWhitespace() -> sb.append(ch)
                    isPunct(ch) -> sb.append(ch)
                    else -> sb.append("__")
                }
            }
            return sb.toString()
        }

        private fun buildHighlightedAnswer(item: DictationQuestion): CharSequence {
            val answer = item.answer
            val userRaw = item.userInput
            val user = buildString {
                userRaw.forEach { ch -> if (!ch.isWhitespace() && !isPunct(ch)) append(ch) }
            }
            val sb = SpannableStringBuilder()
            val colorError = MaterialColors.getColor(itemView.context, com.google.android.material.R.attr.colorError, 0)
            val colorCorrect = MaterialColors.getColor(itemView.context, com.google.android.material.R.attr.colorOnSurface, 0)
            var u = 0
            // Build colored correct answer only (no leading label here)
            answer.forEach { ch ->
                if (ch.isWhitespace() || isPunct(ch)) {
                    sb.append(ch)
                } else {
                    val uc = if (u < user.length) user[u] else '\u0000'
                    val start = sb.length
                    sb.append(ch)
                    val end = sb.length
                    val wrong = (uc == '\u0000') || (uc != ch)
                    sb.setSpan(
                        ForegroundColorSpan(if (wrong) colorError else colorCorrect),
                        start, end,
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    u += 1
                }
            }
            return sb
        }

        private fun isPunct(ch: Char): Boolean {
            val type = Character.getType(ch)
            return when (type) {
                Character.CONNECTOR_PUNCTUATION.toInt(),
                Character.DASH_PUNCTUATION.toInt(),
                Character.START_PUNCTUATION.toInt(),
                Character.END_PUNCTUATION.toInt(),
                Character.INITIAL_QUOTE_PUNCTUATION.toInt(),
                Character.FINAL_QUOTE_PUNCTUATION.toInt(),
                Character.OTHER_PUNCTUATION.toInt() -> true
                else -> "，。？！；：、（）《》【】—…·,.!?;:()<>[]——…·".contains(ch)
            }
        }
    }
}
