package com.jerryz.poems.ui.ai

import com.jerryz.poems.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AiApiClient(
    private val baseUrl: String = BuildConfig.AI_BASE_URL,
    private val apiKey: String = BuildConfig.AI_API_KEY,
    private val model: String = BuildConfig.AI_MODEL
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun createChatCompletion(messages: List<Pair<String, String>>, temperature: Double? = null): String {
        // messages: list of (role, content)
        val json = JSONObject()
        json.put("model", model)
        val arr = JSONArray()
        messages.forEach { (role, content) ->
            arr.put(JSONObject().put("role", role).put("content", content))
        }
        json.put("messages", arr)
        json.put("stream", false)
        json.put("temperature", temperature ?: 0.3)

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$baseUrl/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw RuntimeException("API error: ${resp.code}")
            val resText = resp.body?.string() ?: ""
            val obj = JSONObject(resText)
            val choices = obj.optJSONArray("choices") ?: JSONArray()
            if (choices.length() == 0) return ""
            val msg = choices.getJSONObject(0).optJSONObject("message")
            return msg?.optString("content") ?: ""
        }
    }

    fun createChatCompletionStream(
        messages: List<Pair<String, String>>,
        onDelta: (String) -> Unit
    ) {
        val json = JSONObject()
        json.put("model", model)
        val arr = JSONArray()
        messages.forEach { (role, content) ->
            arr.put(JSONObject().put("role", role).put("content", content))
        }
        json.put("messages", arr)
        json.put("stream", true)
        json.put("temperature", 0.3)

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$baseUrl/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .post(body)
            .build()

        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw RuntimeException("API error: ${resp.code}")
            val source = resp.body?.source() ?: return
            while (true) {
                val line = source.readUtf8Line() ?: break
                if (line.isBlank()) continue
                if (line.startsWith("data:")) {
                    val payload = line.removePrefix("data:").trim()
                    if (payload == "[DONE]") break
                    try {
                        val obj = JSONObject(payload)
                        val choices = obj.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val c0 = choices.getJSONObject(0)
                            val delta = c0.optJSONObject("delta")
                            val token = delta?.optString("content") ?: c0.optJSONObject("message")?.optString("content")
                            if (!token.isNullOrEmpty()) onDelta(token)
                        }
                    } catch (_: Exception) {
                        // ignore malformed chunks
                    }
                }
            }
        }
    }

    fun newChatCompletionStreamCall(
        messages: List<Pair<String, String>>
    ): okhttp3.Call {
        val json = JSONObject()
        json.put("model", model)
        val arr = JSONArray()
        messages.forEach { (role, content) ->
            arr.put(JSONObject().put("role", role).put("content", content))
        }
        json.put("messages", arr)
        json.put("stream", true)
        json.put("temperature", 0.3)

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$baseUrl/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .post(body)
            .build()

        return client.newCall(request)
    }
}
