package com.jerryz.poems.ui.ai

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class DictationQuestion(
    val prompt: String,
    val answer: String,
    val explanation: String,
    var userInput: String = "",
    var result: CheckResult? = null,
    val revision: Long = 0L
)

sealed class CheckResult {
    object Correct : CheckResult()
    data class Typos(
        val total: Int,
        val wrongCount: Int,
        val mismatchMask: List<Boolean>
    ) : CheckResult()
    object WholeWrong : CheckResult()
}

class AiDictationViewModel(
    private val context: Context,
    private val poemId: Int,
    private val title: String,
    private val author: String,
    private val content: String,
    private val translation: String,
    private val api: AiApiClient = AiApiClient(),
    private val dictationRepository: DictationRepository = DictationRepository(context)
) : ViewModel() {

    private val _questions = MutableLiveData<List<DictationQuestion>>(emptyList())
    val questions: LiveData<List<DictationQuestion>> = _questions

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    init {
        // Load saved questions; do not auto-generate
        val saved = dictationRepository.load(poemId)
        _questions.value = saved
    }

    fun generateNewSet(count: Int = 3) {
        if (_loading.value == true) return
        _loading.value = true
        val old = _questions.value.orEmpty()
        viewModelScope.launch {
            val sys = """
你是资深语文教师，擅长命制高考语文诗词默写题。你的任务：基于给定诗词，命制严格、规范、具有区分度的默写题。
仅输出纯JSON，不要任何解释、注释或Markdown。
            """.trimIndent()

            val contextBlock = buildString {
                appendLine("【当前诗词信息】")
                appendLine("标题：$title")
                appendLine("作者：$author")
                appendLine("原文：")
                appendLine(content)
                if (translation.isNotBlank()) {
                    appendLine("\n译文：")
                    appendLine(translation)
                } else {
                    appendLine("\n(暂无译文)")
                }
            }

            val previous = _questions.value.orEmpty().map { it.answer }.filter { it.isNotBlank() }
            val prevBlock = if (previous.isNotEmpty()) {
                "已出过的答案：\n" + previous.joinToString("\n")
            } else "无"

            val user = buildString {
                appendLine("请根据上述诗词，按高考语文风格生成${count}道规范的默写题。")
                appendLine("命题类型包含但不限于：补写下句/上句、根据描述写出句子、补齐名句关键词等；题干不得泄露答案。")
                appendLine("JSON 数组中每题包含以下字段：")
                appendLine("question：题干（不包含答案，语言明确精炼）；也不需要包含填空的横线。")
                appendLine("answer：标准答案，需与原文逐字一致，保留原标点；")
                appendLine("explanation：解析（20-40字），解释选择这一句的原因。")
                appendLine("若可行，请尽量避开与以下已出过的答案重复的句子或考点：")
                appendLine(prevBlock)
                appendLine("只输出纯JSON数组，不要额外文字。")
            }.trim()

            try {
                val result = withContext(Dispatchers.IO) {
                    api.createChatCompletion(
                        listOf(
                            "system" to sys,
                            "user" to contextBlock,
                            "user" to user
                        ),
                        temperature = 0.85
                    )
                }
                val parsed = parseQuestions(result)
                if (parsed.isNotEmpty()) {
                    _questions.value = parsed
                    // Persist only questions; no user answers/results
                    dictationRepository.save(poemId, parsed)
                    _error.value = null
                } else {
                    // Keep old content if parsing failed or empty
                    _questions.value = old
                    _error.value = "生成失败，请重试"
                }
            } catch (e: Exception) {
                // Keep previous content on error
                _questions.value = old
                val fallback = if (e is IllegalStateException) "输出为空，请重试" else "请检查网络后重试"
                val sanitized = AiErrorFormatter.sanitize(e.message, fallback)
                _error.value = "请求失败：$sanitized"
            } finally {
                _loading.value = false
            }
        }
    }

    fun updateUserInput(index: Int, text: String) {
        val list = _questions.value ?: return
        if (index !in list.indices) return
        val q = list[index]
        if (q.userInput == text) return
        // Mutate in place to avoid triggering list diff/rebind and cursor jumps
        q.userInput = text
    }

    fun checkAnswer(index: Int): CheckResult? {
        val list = _questions.value?.toMutableList() ?: return null
        if (index !in list.indices) return null
        val q = list[index]
        val ans = normalizeForCompare(q.answer)
        val user = normalizeForCompare(q.userInput)

        val newRevision = System.nanoTime()

        if (user.isEmpty() || ans.isEmpty()) {
            val result = CheckResult.WholeWrong
            list[index] = q.copy(result = result, revision = newRevision)
            _questions.value = list
            return result
        }

        if (ans == user) {
            val result = CheckResult.Correct
            list[index] = q.copy(result = result, revision = newRevision)
            _questions.value = list
            return result
        }

        val dist = editDistance(ans, user)
        val threshold = maxOf(2, ans.length / 2)
        val result = if (dist > threshold) {
            CheckResult.WholeWrong
        } else {
            CheckResult.Typos(total = ans.length, wrongCount = dist, mismatchMask = emptyList())
        }
        list[index] = q.copy(result = result, revision = newRevision)
        _questions.value = list
        return result
    }

    private fun editDistance(a: String, b: String): Int {
        val n = a.length
        val m = b.length
        if (n == 0) return m
        if (m == 0) return n
        val dp = Array(n + 1) { IntArray(m + 1) }
        for (i in 0..n) dp[i][0] = i
        for (j in 0..m) dp[0][j] = j
        for (i in 1..n) {
            val ca = a[i - 1]
            for (j in 1..m) {
                val cb = b[j - 1]
                val cost = if (ca == cb) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[n][m]
    }

    fun blankMaskOf(answer: String): String {
        val sb = StringBuilder()
        for (ch in answer) {
            if (ch.isWhitespace()) {
                sb.append(ch)
            } else if (isPunctuation(ch)) {
                sb.append(ch)
            } else {
                // two underscores per non-punctuation CJK character
                sb.append("__")
            }
        }
        return sb.toString()
    }

    private fun isPunctuation(ch: Char): Boolean {
        val type = Character.getType(ch)
        return when (type) {
            Character.CONNECTOR_PUNCTUATION.toInt(),
            Character.DASH_PUNCTUATION.toInt(),
            Character.START_PUNCTUATION.toInt(),
            Character.END_PUNCTUATION.toInt(),
            Character.INITIAL_QUOTE_PUNCTUATION.toInt(),
            Character.FINAL_QUOTE_PUNCTUATION.toInt(),
            Character.OTHER_PUNCTUATION.toInt() -> true
            else -> {
                // include common CJK punctuation
                "，。？！；：、（）《》【】“”‘’—…·,.!?;:()<>[]——…·".contains(ch)
            }
        }
    }

    private fun normalizeForCompare(text: String): String {
        // Remove all punctuation and whitespaces
        val sb = StringBuilder()
        text.forEach { ch ->
            if (!ch.isWhitespace() && !isPunctuation(ch)) sb.append(ch)
        }
        return sb.toString()
    }

    private fun parseQuestions(raw: String): List<DictationQuestion> {
        // Attempt to extract the first JSON array in the text
        val start = raw.indexOf('[')
        val end = raw.lastIndexOf(']')
        val jsonText = if (start >= 0 && end > start) raw.substring(start, end + 1) else raw
        return try {
            val arr = JSONArray(jsonText)
            val out = mutableListOf<DictationQuestion>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val q = obj.optString("question").ifBlank { obj.optString("题目") }
                val a = obj.optString("answer").ifBlank { obj.optString("答案") }
                val exp = obj.optString("explanation").ifBlank { obj.optString("解析") }
                if (q.isNotBlank() && a.isNotBlank()) {
                    out += DictationQuestion(prompt = q.trim(), answer = a.trim(), explanation = exp.trim())
                }
            }
            out
        } catch (e: Exception) {
            // Also try JSON object with key
            try {
                val obj = JSONObject(jsonText)
                val arr = obj.optJSONArray("questions") ?: return emptyList()
                val out = mutableListOf<DictationQuestion>()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val q = o.optString("question").ifBlank { o.optString("题目") }
                    val a = o.optString("answer").ifBlank { o.optString("答案") }
                    val exp = o.optString("explanation").ifBlank { o.optString("解析") }
                    if (q.isNotBlank() && a.isNotBlank()) out += DictationQuestion(q.trim(), a.trim(), exp.trim())
                }
                out
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}
