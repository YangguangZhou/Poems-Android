package com.jerryz.poems.ui.ai

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

class ChatRepository(private val context: Context) {
    private val prefs by lazy {
        context.getSharedPreferences("ai_chat_history", Context.MODE_PRIVATE)
    }

    fun loadHistory(poemId: Int): MutableList<ChatMessage> {
        val key = keyFor(poemId)
        val raw = prefs.getString(key, null) ?: return mutableListOf()
        return try {
            val arr = JSONArray(raw)
            MutableList(arr.length()) { idx ->
                val obj = arr.getJSONObject(idx)
                ChatMessage(
                    id = obj.optString("id"),
                    role = if (obj.optString("role") == "user") Role.USER else Role.ASSISTANT,
                    content = obj.optString("content"),
                    timestamp = obj.optLong("timestamp")
                )
            }
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun saveHistory(poemId: Int, list: List<ChatMessage>) {
        val arr = JSONArray()
        list.forEach { msg ->
            val obj = JSONObject()
                .put("id", msg.id)
                .put("role", if (msg.role == Role.USER) "user" else "assistant")
                .put("content", msg.content)
                .put("timestamp", msg.timestamp)
            arr.put(obj)
        }
        prefs.edit(commit = true) {
            putString(keyFor(poemId), arr.toString())
        }
    }

    fun deleteMessage(poemId: Int, messageId: String) {
        val list = loadHistory(poemId)
        val newList = list.filterNot { it.id == messageId }
        saveHistory(poemId, newList)
    }

    private fun keyFor(poemId: Int) = "chat_history_$poemId"
}

