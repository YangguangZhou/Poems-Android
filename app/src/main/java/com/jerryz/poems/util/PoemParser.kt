package com.jerryz.poems.util

import com.jerryz.poems.data.Poem
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * 解析诗词文本文件
 */
class PoemParser {

    /**
     * 从输入流解析诗词
     */
    fun parsePoems(inputStream: InputStream): List<Poem> {
        val poems = mutableListOf<Poem>()

        // 读取所有文本内容
        val text = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }

        // 标准化换行符并清除首尾空白
        val normalizedText = text.replace("\r\n", "\n").trim()

        // 按三个换行符分割诗词
        val poemsData = normalizedText.split("\n\n\n")

        // 解析每首诗
        poemsData.forEachIndexed { index, poemText ->
            // 按行分割
            val lines = poemText.split("\n")

            if (lines.size >= 3) {
                val title = lines[0].trim()
                val author = lines[1].trim()
                val tagsLine = lines[2].trim()

                // 解析标签
                val tags = if (tagsLine.startsWith("Tags:")) {
                    tagsLine.substring(5).split(",").map { it.trim() }
                } else {
                    emptyList()
                }

                val content = mutableListOf<String>()
                val translation = mutableListOf<String>()
                var section = "content"

                // 从第4行开始解析内容和翻译
                for (i in 3 until lines.size) {
                    if (lines[i].trim().isEmpty()) {
                        section = "translation"
                        continue
                    }

                    if (section == "content") {
                        content.add(lines[i].trim())
                    } else {
                        translation.add(lines[i].trim())
                    }
                }

                poems.add(
                    Poem(
                        id = index,
                        title = title,
                        author = author,
                        tags = tags,
                        content = content,
                        translation = translation
                    )
                )
            }
        }

        return poems
    }
}