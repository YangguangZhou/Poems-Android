package com.jerryz.poems.ui.ai

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

class DictationRepository(private val context: Context) {
    private val prefs by lazy {
        context.getSharedPreferences("ai_dictation_questions", Context.MODE_PRIVATE)
    }

    fun load(poemId: Int): List<DictationQuestion> {
        val raw = prefs.getString(key(poemId), null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val out = mutableListOf<DictationQuestion>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val q = obj.optString("question")
                val a = obj.optString("answer")
                val e = obj.optString("explanation")
                if (q.isNotBlank() && a.isNotBlank()) {
                    out += DictationQuestion(prompt = q, answer = a, explanation = e)
                }
            }
            out
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(poemId: Int, list: List<DictationQuestion>) {
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
                .put("question", item.prompt)
                .put("answer", item.answer)
                .put("explanation", item.explanation)
            arr.put(obj)
        }
        prefs.edit(commit = true) {
            putString(key(poemId), arr.toString())
        }
    }

    private fun key(id: Int) = "dictation_$id"
}

