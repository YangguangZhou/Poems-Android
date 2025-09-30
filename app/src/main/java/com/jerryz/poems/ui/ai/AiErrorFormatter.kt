package com.jerryz.poems.ui.ai

import com.jerryz.poems.BuildConfig

/**
 * Sanitizes AI error messages to avoid leaking internal endpoints and keeps a readable fallback.
 */
object AiErrorFormatter {

    private const val DEFAULT_FALLBACK = "未知错误"

    private val replacements: List<Pair<String, String>> by lazy { buildReplacements() }

    private fun buildReplacements(): List<Pair<String, String>> {
        val raw = BuildConfig.AI_ERROR_FILTER_DOMAIN.orEmpty().trim()
        if (raw.isEmpty()) return emptyList()
        val normalized = raw
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("//")
            .trim('/')
        if (normalized.isEmpty()) return emptyList()
        return listOf(
            "https://$normalized/" to "API/",
            "http://$normalized/" to "API/",
            "https://$normalized" to "API",
            "http://$normalized" to "API",
            "$normalized/" to "API/",
            normalized to "API"
        )
    }

    fun sanitize(rawMessage: String?, fallback: String = DEFAULT_FALLBACK): String {
        val base = rawMessage?.takeUnless { it.isBlank() } ?: fallback
        if (replacements.isEmpty()) return base.trim().ifEmpty { fallback }
        var result = base
        replacements.forEach { (old, new) ->
            result = result.replace(old, new, ignoreCase = true)
        }
        return result.trim().ifEmpty { fallback }
    }
}

